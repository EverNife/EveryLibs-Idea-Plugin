package dev.petrus.everylibs.idea;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import java.util.List;

/**
 * Functional tests over the reference contributor. Stubs of the recognized API are added to the
 * in-memory project so a call resolves to the right declaring class; a sample target supplies the
 * members completion/resolve should find.
 */
public class EveryLibsReferenceTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addClass(
                "package br.com.finalcraft.everylibs.reflection.lookup;\n" +
                "public final class FieldReflection {\n" +
                "  public static final FieldReflection INSTANCE = new FieldReflection();\n" +
                "  public <T> Object getField(Class<?> target, String name) { return null; }\n" +
                "}\n");
        myFixture.addClass(
                "package br.com.finalcraft.everylibs.reflection.lookup;\n" +
                "public final class MethodReflection {\n" +
                "  public static final MethodReflection INSTANCE = new MethodReflection();\n" +
                "  public <T> Object getMethod(Class<?> clazz, String name, Class<?>... params) { return null; }\n" +
                "}\n");
        myFixture.addClass(
                "package sample;\n" +
                "public class Target {\n" +
                "  public String name;\n" +
                "  private int health;\n" +
                "  public void setName(String n) {}\n" +
                "  public int getHealth() { return health; }\n" +
                "}\n");
    }

    public void testFieldNameCompletionIncludesPrivateInherited() {
        myFixture.configureByText("Use.java",
                "import br.com.finalcraft.everylibs.reflection.lookup.FieldReflection;\n" +
                "import sample.Target;\n" +
                "class Use {\n" +
                "  void m() { FieldReflection.INSTANCE.getField(Target.class, \"<caret>\"); }\n" +
                "}\n");
        myFixture.complete(CompletionType.BASIC);
        List<String> names = myFixture.getLookupElementStrings();
        assertNotNull(names);
        assertTrue("expected public field 'name'", names.contains("name"));
        // The point of the library: a visible-but-inaccessible private field is still offered.
        assertTrue("expected private field 'health'", names.contains("health"));
    }

    public void testMethodCompletionOffersDeclaredButNotObjectMembers() {
        myFixture.configureByText("Use.java",
                "import br.com.finalcraft.everylibs.reflection.lookup.MethodReflection;\n" +
                "import sample.Target;\n" +
                "class Use {\n" +
                "  void m() { MethodReflection.INSTANCE.getMethod(Target.class, \"<caret>\"); }\n" +
                "}\n");
        myFixture.complete(CompletionType.BASIC);
        List<String> names = myFixture.getLookupElementStrings();
        assertNotNull(names);
        assertTrue(names.contains("setName"));
        assertTrue(names.contains("getHealth"));
        // java.lang.Object members are noise in the popup and must be filtered from completion.
        assertFalse("Object's wait() should not be offered", names.contains("wait"));
        assertFalse("Object's hashCode() should not be offered", names.contains("hashCode"));
    }

    public void testMethodNameResolvesForNavigation() {
        myFixture.configureByText("Use.java",
                "import br.com.finalcraft.everylibs.reflection.lookup.MethodReflection;\n" +
                "import sample.Target;\n" +
                "class Use {\n" +
                "  void m() { MethodReflection.INSTANCE.getMethod(Target.class, \"setNa<caret>me\"); }\n" +
                "}\n");
        PsiReference reference = myFixture.getReferenceAtCaretPosition();
        assertNotNull(reference);
        assertTrue(reference.resolve() instanceof PsiMethod);
        assertEquals("setName", ((PsiMethod) reference.resolve()).getName());
    }

    public void testClassNameArgumentResolvesLikeForName() {
        myFixture.addClass(
                "package br.com.finalcraft.everylibs.reflection.lookup;\n" +
                "public final class ClassReflection {\n" +
                "  public static final ClassReflection INSTANCE = new ClassReflection();\n" +
                "  public Class<?> getClass(String lookupName) { return null; }\n" +
                "}\n");
        myFixture.addClass("package sample;\npublic class Widget {}\n");
        // Caret on the last char of the class name so we land inside the final segment's reference
        // (JavaClassReferenceProvider builds one reference per dotted segment).
        myFixture.configureByText("Use.java",
                "import br.com.finalcraft.everylibs.reflection.lookup.ClassReflection;\n" +
                "class Use {\n" +
                "  void m() { ClassReflection.INSTANCE.getClass(\"sample.Widget<caret>\"); }\n" +
                "}\n");
        PsiReference reference = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset() - 1);
        assertNotNull("class-name string should contribute a reference", reference);
        PsiElement resolved = reference.resolve();
        assertTrue("class-name string should resolve to a PsiClass", resolved instanceof PsiClass);
        assertEquals("sample.Widget", ((PsiClass) resolved).getQualifiedName());
    }

    public void testFacadeStyleQualifierStillMatchesOnDeclaringClass() {
        // Reached through a wrapper method whose return type is FieldReflection: the match is on the
        // DECLARING class of getField, not on the qualifier expression.
        myFixture.addClass(
                "package sample;\n" +
                "import br.com.finalcraft.everylibs.reflection.lookup.FieldReflection;\n" +
                "public final class FCReflectionUtil {\n" +
                "  public static FieldReflection fields() { return FieldReflection.INSTANCE; }\n" +
                "}\n");
        // Empty prefix keeps the lookup popup open (more than one field), so the variant list is
        // observable instead of being auto-inserted on a single match.
        myFixture.configureByText("Use.java",
                "import sample.FCReflectionUtil;\n" +
                "import sample.Target;\n" +
                "class Use {\n" +
                "  void m() { FCReflectionUtil.fields().getField(Target.class, \"<caret>\"); }\n" +
                "}\n");
        myFixture.complete(CompletionType.BASIC);
        List<String> names = myFixture.getLookupElementStrings();
        assertNotNull(names);
        assertTrue(names.contains("name"));
    }
}
