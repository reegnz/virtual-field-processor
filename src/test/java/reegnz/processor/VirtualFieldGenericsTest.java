package reegnz.processor;

import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import javax.tools.JavaFileObject;

import org.junit.Test;

import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;

public class VirtualFieldGenericsTest {

	@Test
	public void shouldGenerateWithCustomName() {
		JavaFileObject input = JavaFileObjects.forSourceLines("example.GenericsIfc",
				"package example;",
				"",
				"import reegnz.processor.api.VirtualField;",
				"",
				"@VirtualField",
				"public interface GenericsIfc<A, B extends Iterable<A>> {",
				"    B hello(A target);",
				"",
				"    Iterable<?> testWildcard(Iterable<?> list);",
				"",
				"    Iterable<? extends A> testBounds(Iterable<? super A> list);",
				"",
				"    <Z extends Iterable<?>> Iterable<Z> testMethodGenerics();",
				"}");
			JavaFileObject generated = JavaFileObjects.forSourceLines("example.VirtualGenericsIfc",
				"package example;",
				"",
				"import java.lang.Iterable;",
				"import java.lang.Override;",
				"import javax.annotation.Generated;",
				"",
				"@Generated(\"reegnz.processor.VirtualFieldProcessor\")",
				"public interface VirtualGenericsIfc<A, B extends Iterable<A>> extends GenericsIfc<A, B> {",
				"    GenericsIfc<A, B> getGenericsIfc();",
				"",
				"    @Override",
				"    default B hello(A target) {",
				"        return getGenericsIfc().hello(target);",
				"    }",
				"",
				"    @Override",
				"    default Iterable<?> testWildcard(Iterable<?> list) {",
				"        return getGenericsIfc().testWildcard(list);",
				"    }",
				"",
				"    @Override",
				"    default Iterable<? extends A> testBounds(Iterable<? super A> list) {",
				"        return getGenericsIfc().testBounds(list);",
				"    }",
				"",
				"    @Override",
				"    default <Z extends Iterable<?>> Iterable<Z> testMethodGenerics() {",
				"        return getGenericsIfc().testMethodGenerics();",
				"    }",
				"}");
			Truth.assertAbout(javaSource())
				.that(input)
				.processedWith(new VirtualFieldProcessor())
				.compilesWithoutError().and()
				.generatesSources(generated);
	}

}
