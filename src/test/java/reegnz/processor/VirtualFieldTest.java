package reegnz.processor;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import javax.tools.JavaFileObject;

import org.junit.Test;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;

public class VirtualFieldTest {

	@Test
	public void shouldCompileAndGenerate() {
		JavaFileObject input = JavaFileObjects.forSourceLines("example.MyIfc",
			"package example;",
			"",
			"import reegnz.processor.api.VirtualField;",
			"",
			"@VirtualField",
			"public interface MyIfc {",
			"",
			"    void myMethod();",
			"",
			"    int myOtherMethod(String name);",
			"",
			"    default String aDefaultMethod(String a, String b, String c) {",
			"        return a + b + c;",
			"    }",
			"",
			"    static void staticMethod() {",
			"    }",
			"",
			"}");
		JavaFileObject generated = JavaFileObjects.forSourceLines("example.VirtualMyIfc",
			"package example;",
			"",
			"import java.lang.Override;",
			"import java.lang.String;",
			"",
			"public interface VirtualMyIfc extends MyIfc {",
			"    MyIfc getMyIfc();",
			"",
			"    @Override",
			"    default void myMethod() {",
			"        getMyIfc().myMethod();",
			"    }",
			"",
			"    @Override",
			"    default int myOtherMethod(String name) {",
			"        return getMyIfc().myOtherMethod(name);",
			"    }",
			"",
			"    @Override",
			"    default String aDefaultMethod(String a, String b, String c) {",
			"        return getMyIfc().aDefaultMethod(a, b, c);",
			"    }",
			"",
			"}");
		Truth.assertAbout(javaSource())
			.that(input)
			.processedWith(new VirtualFieldProcessor())
			.compilesWithoutError().and()
			.generatesSources(generated);
	}

	@Test
	public void voidMethod() {
		JavaFileObject input = JavaFileObjects.forSourceLines("example.MyIfc",
				"package example;",
				"",
				"import reegnz.processor.api.VirtualField;",
				"",
				"@VirtualField",
				"public interface MyIfc {",
				"",
				"    void myMethod();",
				"",
				"}");
			JavaFileObject generated = JavaFileObjects.forSourceLines("example.VirtualMyIfc",
				"package example;",
				"",
				"import java.lang.Override;",
				"",
				"public interface VirtualMyIfc extends MyIfc {",
				"    MyIfc getMyIfc();",
				"",
				"    @Override",
				"    default void myMethod() {",
				"        getMyIfc().myMethod();",
				"    }",
				"",
				"}");
			Truth.assertAbout(javaSource())
				.that(input)
				.processedWith(new VirtualFieldProcessor())
				.compilesWithoutError().and()
				.generatesSources(generated);
	}

	public void returnMethodWithArgs() {
		JavaFileObject input = JavaFileObjects.forSourceLines("example.MyIfc",
			"package example;",
			"",
			"import reegnz.processor.api.VirtualField;",
			"",
			"@VirtualField",
			"public interface MyIfc {",
			"",
			"    String methodWithParameters(String a, String b, String c);",
			"",
			"}");
		JavaFileObject generated = JavaFileObjects.forSourceLines("example.VirtualMyIfc",
			"package example;",
			"",
			"import java.lang.Override;",
			"import java.lang.String;",
			"",
			"public interface VirtualMyIfc extends MyIfc {",
			"    MyIfc getMyIfc();",
			"",
			"    @Override",
			"    default String methodWithParameters(String a, String b, String c) {",
			"        return getMyIfc().methodWithParameters(a, b, c);",
			"    }",
			"",
			"}");
			Truth.assertAbout(javaSource())
				.that(input)
				.processedWith(new VirtualFieldProcessor())
				.compilesWithoutError().and()
				.generatesSources(generated);
	}

	@Test
	public void shouldOverrideDefaultMethod() {
		JavaFileObject input = JavaFileObjects.forSourceLines("example.MyIfc",
			"package example;",
			"",
			"import reegnz.processor.api.VirtualField;",
			"",
			"@VirtualField",
			"public interface MyIfc {",
			"",
			"    default String methodWithParameters(String a, String b, String c) {",
			"        return a + b + c;",
			"    }",
			"",
			"}");
		JavaFileObject generated = JavaFileObjects.forSourceLines("example.VirtualMyIfc",
			"package example;",
			"",
			"import java.lang.Override;",
			"import java.lang.String;",
			"",
			"public interface VirtualMyIfc extends MyIfc {",
			"    MyIfc getMyIfc();",
			"",
			"    @Override",
			"    default String methodWithParameters(String a, String b, String c) {",
			"        return getMyIfc().methodWithParameters(a, b, c);",
			"    }",
			"",
			"}");
			Truth.assertAbout(javaSource())
				.that(input)
				.processedWith(new VirtualFieldProcessor())
				.compilesWithoutError().and()
				.generatesSources(generated);
	}

	@Test
	public void shouldNotOverrideStaticMethod() {
		JavaFileObject input = JavaFileObjects.forSourceLines("example.MyIfc",
			"package example;",
			"",
			"import reegnz.processor.api.VirtualField;",
			"",
			"@VirtualField",
			"public interface MyIfc {",
			"    static void staticMethod() {",
			"    }",
			"",
			"}");
		JavaFileObject generated = JavaFileObjects.forSourceLines("example.VirtualMyIfc",
			"package example;",
			"",
			"public interface VirtualMyIfc extends MyIfc {",
			"    MyIfc getMyIfc();",
			"}");
		Truth.assertAbout(javaSource())
			.that(input)
			.processedWith(new VirtualFieldProcessor())
			.compilesWithoutError().and()
			.generatesSources(generated);
	}

	@Test
	public void shouldGenerateWithCustomName() {
		JavaFileObject input = JavaFileObjects.forSourceLines("example.MyIfc",
				"package example;",
				"",
				"import reegnz.processor.api.VirtualField;",
				"",
				"@VirtualField(\"MyCustomName\")",
				"public interface MyIfc {",
				"    static void staticMethod() {",
				"    }",
				"",
				"}");
			JavaFileObject generated = JavaFileObjects.forSourceLines("example.MyCustomName",
				"package example;",
				"",
				"public interface MyCustomName extends MyIfc {",
				"    MyIfc getMyIfc();",
				"}");
			Truth.assertAbout(javaSource())
				.that(input)
				.processedWith(new VirtualFieldProcessor())
				.compilesWithoutError().and()
				.generatesSources(generated);
	}
}
