package reegnz.processor;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import javax.tools.JavaFileObject;

import org.junit.Test;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;

public class VirtualFieldObjectMethodsTest {

	@Test
	public void shouldSkipObjectMethods() {
		JavaFileObject input = JavaFileObjects.forSourceLines("example.ObjectIfc",
				"package example;",
				"",
				"import reegnz.processor.api.VirtualField;",
				"",
				"@VirtualField",
				"public interface ObjectIfc {",
				"    String hello(String target);",
				"",
				"    String toString();",
				"",
				"    int hashCode();",
				"",
				"    boolean equals(Object obect);",
				"}");
			JavaFileObject generated = JavaFileObjects.forSourceLines("example.ObjectIfc",
				"package example;",
				"",
				"import java.lang.Override;",
				"import java.lang.String",
				"import javax.annotation.Generated;",
				"",
				"@Generated(\"reegnz.processor.VirtualFieldProcessor\")",
				"public interface VirtualObjectIfc extends ObjectIfc {",
				"    ObjectIfc getObjectIfc();",
				"",
				"    @Override",
				"    default String hello(String target) {",
				"        return getObjectIfc().hello(target);",
				"    }",
				"}");
			Truth.assertAbout(javaSource())
				.that(input)
				.processedWith(new VirtualFieldProcessor())
				.compilesWithoutError().and()
				.generatesSources(generated);
	}
	
}
