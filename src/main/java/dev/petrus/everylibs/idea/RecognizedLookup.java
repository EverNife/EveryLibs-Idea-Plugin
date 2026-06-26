package dev.petrus.everylibs.idea;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The single source of truth for the EveryLibs reflection API surface the plugin understands.
 * <p>
 * Each entry maps a (declaring class FQN + method name) to where the <b>target class</b> comes from
 * and which argument carries the <b>member name</b>. Matching on the <em>declaring</em> class is what
 * makes the singleton path ({@code FieldReflection.INSTANCE.getField(...)}) and the facade path
 * ({@code FCReflectionUtil.getFields().getField(...)}) behave identically — both resolve to a method
 * declared on {@code FieldReflection}.
 * <p>
 * To grow coverage, add a row here; nothing else needs to know the API shape.
 */
final class RecognizedLookup {

    /** Where the target {@link com.intellij.psi.PsiClass} is found for a recognized call. */
    enum TargetSource {
        /** Argument 0 is a {@code SomeClass.class} literal (or a {@code Class<X>}-typed expression). */
        CLASS_ARG0,
        /** The call's qualifier has type {@code ClassReflect<C>}; the target is the generic {@code C}. */
        RECEIVER_GENERIC
    }

    private static final String LOOKUP_PKG = "br.com.finalcraft.everylibs.reflection.lookup.";
    private static final String ROOT_PKG = "br.com.finalcraft.everylibs.reflection.";

    static final String FIELD_REFLECTION = LOOKUP_PKG + "FieldReflection";
    static final String METHOD_REFLECTION = LOOKUP_PKG + "MethodReflection";
    static final String CLASS_REFLECT = ROOT_PKG + "ClassReflect";

    private static final List<RecognizedLookup> ALL = new ArrayList<>();

    static {
        // target is SomeClass.class at arg 0, member name at arg 1
        ALL.add(new RecognizedLookup(FIELD_REFLECTION, "getField", 1, MemberKind.FIELD, TargetSource.CLASS_ARG0));
        ALL.add(new RecognizedLookup(METHOD_REFLECTION, "getMethod", 1, MemberKind.METHOD, TargetSource.CLASS_ARG0));
        ALL.add(new RecognizedLookup(METHOD_REFLECTION, "getTypedMethod", 1, MemberKind.METHOD, TargetSource.CLASS_ARG0));
        // fluent ClassReflect<C>: target is the receiver's generic, member name at arg 0
        ALL.add(new RecognizedLookup(CLASS_REFLECT, "field", 0, MemberKind.FIELD, TargetSource.RECEIVER_GENERIC));
        ALL.add(new RecognizedLookup(CLASS_REFLECT, "method", 0, MemberKind.METHOD, TargetSource.RECEIVER_GENERIC));
        ALL.add(new RecognizedLookup(CLASS_REFLECT, "typedMethod", 0, MemberKind.METHOD, TargetSource.RECEIVER_GENERIC));
    }

    final String declaringClassFqn;
    final String methodName;
    final int nameArgIndex;
    final MemberKind memberKind;
    final TargetSource targetSource;

    private RecognizedLookup(String declaringClassFqn, String methodName, int nameArgIndex,
                             MemberKind memberKind, TargetSource targetSource) {
        this.declaringClassFqn = declaringClassFqn;
        this.methodName = methodName;
        this.nameArgIndex = nameArgIndex;
        this.memberKind = memberKind;
        this.targetSource = targetSource;
    }

    /**
     * @return the matching entry, or {@code null} if this method is not a recognized lookup.
     */
    @Nullable
    static RecognizedLookup find(String declaringClassFqn, String methodName) {
        for (RecognizedLookup lookup : ALL) {
            if (lookup.declaringClassFqn.equals(declaringClassFqn) && lookup.methodName.equals(methodName)) {
                return lookup;
            }
        }
        return null;
    }
}
