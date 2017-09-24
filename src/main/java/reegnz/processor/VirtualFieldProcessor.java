package reegnz.processor;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import reegnz.processor.api.VirtualField;

@AutoService(Processor.class)
public class VirtualFieldProcessor extends AbstractProcessor {

	private Elements elementUtils;
	private Filer filer;
	private Messager messager;
	private Set<String> objectMethods;
	private boolean debug;

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_8;
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> result = new HashSet<String>();
		result.add(VirtualField.class.getCanonicalName());
		return result;
	}

	@Override
	public Set<String> getSupportedOptions() {
		return new HashSet<>(Arrays.asList("debug"));
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		elementUtils = processingEnv.getElementUtils();
		filer = processingEnv.getFiler();
		messager = processingEnv.getMessager();
		objectMethods = getObjectMethodSignatures();
		debug = processingEnv.getOptions().containsKey("debug");
	}

	/**
	 * Gets all java.lang.Object methods that are are possible to override.
	 * @param name
	 * @return
	 */
	private Set<String> getObjectMethodSignatures() {
		return elementUtils.getTypeElement("java.lang.Object")
				.getEnclosedElements().stream()
				.filter(e -> e.getKind().equals(ElementKind.METHOD))
				.map(ExecutableElement.class::cast)
				.filter(e -> !e.getModifiers().contains(Modifier.STATIC))
				.filter(e -> !e.getModifiers().contains(Modifier.FINAL))
				.map(ExecutableElement::toString)
				.collect(Collectors.toSet());
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (!roundEnv.processingOver()) {
			processAnnotations(annotations, roundEnv);
		}
		return false;
	}

	private void processAnnotations(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		annotations.stream().forEach(type -> type.getKind().compareTo(ElementKind.ANNOTATION_TYPE));
		roundEnv.getElementsAnnotatedWith(VirtualField.class).stream().forEach(this::processElement);
	}

	private void processElement(Element element) {
		if (!element.getKind().equals(ElementKind.INTERFACE)) {
			AnnotationMirror annotationMirror = getAnnotationMirror(element, VirtualField.class);
			error(element, annotationMirror, "The annotation @%s is disallowed for this location.",
					VirtualField.class.getSimpleName());
			return;
		}
		procesTypeElement(TypeElement.class.cast(element));
	}

	private void procesTypeElement(TypeElement element) {
		note("Processing type %s", element);
		List<ExecutableElement> methods = element.getEnclosedElements().stream()
				.filter(this::isMethod)
				.map(ExecutableElement.class::cast)
				.filter(e -> !objectMethods.contains(e.toString()))
				.filter(this::isNotStatic)
				.collect(toList());
		generate(element, methods);
	}

	private boolean isMethod(Element element) {
		return ElementKind.METHOD.equals(element.getKind());
	}

	private boolean isNotStatic(ExecutableElement element) {
		return !element.getModifiers().contains(Modifier.STATIC);
	}

	private void generate(TypeElement type, List<ExecutableElement> methods) {
		TypeSpec typeSpec = getTypeSpec(type).addMethods(getMethodSpecs(type, methods)).build();
		String packageName = elementUtils.getPackageOf(type).getQualifiedName().toString();
		JavaFile javaFile = JavaFile.builder(packageName, typeSpec).indent("    ").build();
		String typeName = javaFile.packageName + "." + javaFile.typeSpec.name;
		try {
			note("Writing source of %s", typeName);
			javaFile.writeTo(filer);
		} catch (IOException e) {
			error("Failed to write class %s. Reason: %s", typeName, e);
		}
	}

	private TypeSpec.Builder getTypeSpec(TypeElement type) {
		return TypeSpec.interfaceBuilder(getNewClassName(type))
				.addTypeVariables(getTypeVariables(type))
				.addModifiers(Modifier.PUBLIC)
				.addSuperinterface(TypeName.get(type.asType()))
				.addAnnotation(generatedAnnotation())
				.addOriginatingElement(type);
	}

	private AnnotationSpec generatedAnnotation() {
		return AnnotationSpec.builder(Generated.class)
		.addMember("value", "$S", this.getClass().getName())
		.build();
	}

	private ClassName getNewClassName(TypeElement type) {
		String newIfcName = getNewInterfaceName(type);
		String packageName = elementUtils.getPackageOf(type).getQualifiedName().toString();
		ClassName className = ClassName.get(packageName, newIfcName);
		note("Generated class name is %s", className);
		return className;
	}

	private Iterable<TypeVariableName> getTypeVariables(TypeElement type) {
		Iterable<TypeVariableName> typeVariables = type.getTypeParameters().stream()
				.map(el -> TypeVariableName.get(el))
				.collect(Collectors.toList());
		note("Type variables for class are: %s", typeVariables);
		return typeVariables;
	}

	private String getNewInterfaceName(TypeElement type) {
		String sourceName = getClassName(type);
		AnnotationValue annotationValue = getAnnotationValue(type, VirtualField.class, "value");
		String newIfcName = (String) annotationValue.getValue();
		if(newIfcName.isEmpty()) {
			newIfcName = "Virtual" + sourceName;
		}
		return newIfcName;
	}

	private List<MethodSpec> getMethodSpecs(TypeElement type, List<ExecutableElement> methods) {
		TypeName superTypeName = TypeName.get(type.asType());
		String delegateMethodName = "get" + type.getSimpleName();
		List<MethodSpec> specs = new ArrayList<>();
		MethodSpec delegate = MethodSpec.methodBuilder(delegateMethodName)
				.addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
				.returns(superTypeName)
				.build();
		specs.add(delegate);
		methods.stream()
				.map(method -> convertToSpec(method, delegateMethodName))
				.collect(toCollection(() -> specs));
		note("The following methods will have default methods generated for them: %s", methods);
		return specs;
	}

	private MethodSpec convertToSpec(ExecutableElement method, String delegateMethodName) {
		return MethodSpec.overriding(method)
				.addModifiers(Modifier.DEFAULT)
				.addStatement(
						createStatement(method),
						statementArgs(method, delegateMethodName))
				.build();
	}

	private String createStatement(ExecutableElement method) {
		String argLiterals = method.getParameters().stream()
				.map(e -> "$L")
				.collect(joining(", "));
		String statement = "$L().$L(" + argLiterals + ")";
		if (!returnsVoid(method)) {
			statement = "return " + statement;
		}
		return statement;
	}

	private Object[] statementArgs(ExecutableElement method, String delegateMethodName) {
		List<Name> argNames = method.getParameters().stream()
				.map(el -> el.getSimpleName())
				.collect(Collectors.toList());
		ArrayList<Object> argsList = new ArrayList<>();
		argsList.add(delegateMethodName);
		argsList.add(method.getSimpleName().toString());
		argsList.addAll(argNames);
		return argsList.toArray();
	}

	private String getClassName(TypeElement type) {
		Stack<TypeElement> types = new Stack<>();
		while(true) {
			types.add(type);
			Element enclosingElement = type.getEnclosingElement();
			if(enclosingElement.getKind().equals(ElementKind.PACKAGE)) {
				return types.stream()
						.map(element -> element.getSimpleName().toString())
						.collect(Collectors.joining("."));
			}
			type = TypeElement.class.cast(enclosingElement);
		}
	}

	private AnnotationMirror getAnnotationMirror(Element element, Class<? extends Annotation> annotationClass) {
		for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
			if (annotationMirror.getAnnotationType().toString().equals(annotationClass.getName())) {
				return annotationMirror;
			}
		}
		return null;
	}

	private AnnotationValue getAnnotationValue(Element element, Class<? extends Annotation> annotationClass,
			String key) {
		AnnotationMirror annotationMirror = getAnnotationMirror(element, annotationClass);
		return getAnnotationValue(annotationMirror, key);
	}

	private AnnotationValue getAnnotationValue(AnnotationMirror annotationMirror, String key) {
		Map<String, AnnotationValue> map = elementUtils.getElementValuesWithDefaults(annotationMirror).entrySet()
				.stream()
				.collect(
						Collectors.toMap(
								e -> e.getKey().getSimpleName().toString(),
								e -> e.getValue()));
		return map.get(key);
	}

	private boolean returnsVoid(ExecutableElement method) {
		return method.getReturnType().getKind().equals(TypeKind.VOID);
	}

	private void error(String message, Object...args) {
		messager.printMessage(Kind.ERROR, formatMessage(message, args));
	}

	private void error(Element element, AnnotationMirror annotation, String message, Object...args) {
		messager.printMessage(Kind.ERROR, formatMessage(message, args), element, annotation);
	}

	private void note(String message, Object...args) {
		if(!debug) {
			return;
		}
		messager.printMessage(Kind.NOTE, formatMessage(message, args));
	}

	private String formatMessage(String message, Object... args) {
		return String.format("VirtualFieldProcessor: " + message, args);
	}
}
