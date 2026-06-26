package dev.petrus.everylibs.idea;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Class-name string arguments — the {@code Class.forName("…")} analogue for EveryLibs lookups that
 * take a fully-qualified class name as a String ({@code ClassReflection.getClass}/{@code getFirstClass}/
 * {@code getUntypedClass}/{@code isClassLoaded}, {@code ClassReflect.of(String)}, and the
 * {@code getField(String className, …)} / {@code getMethod(String className, …)} overloads).
 * <p>
 * These are delegated to the platform's {@link JavaClassReferenceProvider}, which already supplies
 * package/class completion, navigation and rename — the same machinery IntelliJ uses for
 * {@code Class.forName}.
 */
final class ClassNameSupport {

    private static final JavaClassReferenceProvider PROVIDER = new JavaClassReferenceProvider();

    private static final String CLASS_REFLECTION = "br.com.finalcraft.everylibs.reflection.lookup.ClassReflection";
    private static final Set<String> CLASS_REFLECTION_METHODS = new HashSet<>(Arrays.asList(
            "getClass", "getUntypedClass", "isClassLoaded", "getFirstClass"));

    private ClassNameSupport() {
    }

    /**
     * @return whether the argument at {@code argIndex} of {@code method} carries a class-name string.
     */
    static boolean isClassNameArgument(String declaringFqn, PsiMethod method, int argIndex) {
        String name = method.getName();
        boolean recognized =
                (CLASS_REFLECTION.equals(declaringFqn) && CLASS_REFLECTION_METHODS.contains(name))
                        || (RecognizedLookup.CLASS_REFLECT.equals(declaringFqn) && name.equals("of"))
                        || (RecognizedLookup.FIELD_REFLECTION.equals(declaringFqn) && name.equals("getField"))
                        || (RecognizedLookup.METHOD_REFLECTION.equals(declaringFqn)
                                && (name.equals("getMethod") || name.equals("getMethods")));
        if (!recognized) {
            return false;
        }
        if (!isStringType(effectiveParameterType(method, argIndex))) {
            return false;
        }
        // getFirstClass takes a String... of candidate names — every String argument is a class name.
        // Every other recognized method carries the class name only as its first argument (the String
        // overload); a String at index 1 there is a member name, handled elsewhere, not a class name.
        boolean varargCandidates = CLASS_REFLECTION.equals(declaringFqn) && name.equals("getFirstClass");
        return varargCandidates || argIndex == 0;
    }

    static PsiReference[] referencesFor(PsiLiteralExpression literal) {
        if (!(literal.getValue() instanceof String)) {
            return PsiReference.EMPTY_ARRAY;
        }
        String text = (String) literal.getValue();
        int startInElement = ElementManipulators.getValueTextRange(literal).getStartOffset();
        // Build the per-segment reference set directly (a.b.C -> refs for package a, a.b, class C) —
        // the same construction Class.forName uses for class-name strings.
        return new JavaClassReferenceSet(text, literal, startInElement, false, PROVIDER).getAllReferences();
    }

    private static boolean isStringType(PsiType type) {
        if (type == null) {
            return false;
        }
        // Resolved code yields java.lang.String; fall back to the short name when the type cannot be
        // resolved to its FQN (shallow scope / test fixtures).
        return type.equalsToText(CommonClassNames.JAVA_LANG_STRING) || "String".equals(type.getPresentableText());
    }

    private static PsiType effectiveParameterType(PsiMethod method, int argIndex) {
        PsiParameter[] parameters = method.getParameterList().getParameters();
        if (parameters.length == 0 || argIndex < 0) {
            return null;
        }
        if (argIndex < parameters.length - 1) {
            return parameters[argIndex].getType();
        }
        PsiParameter last = parameters[parameters.length - 1];
        PsiType lastType = last.getType();
        // A trailing String... maps every index at or beyond the varargs slot to its component type.
        if (last.isVarArgs() && lastType instanceof PsiArrayType) {
            return ((PsiArrayType) lastType).getComponentType();
        }
        return argIndex == parameters.length - 1 ? lastType : null;
    }
}
