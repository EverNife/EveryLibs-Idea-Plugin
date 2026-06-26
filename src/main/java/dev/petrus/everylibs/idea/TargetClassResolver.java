package dev.petrus.everylibs.idea;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the target {@link PsiClass} of a recognized lookup from a single {@link PsiExpression},
 * collapsing the spec's v1/v2 split on the target axis: {@code X.class}, a {@code Class<X>}-typed
 * expression, and the receiver of a {@code ClassReflect<C>} all reduce to the same "expression →
 * class" operation (mirroring IntelliJ's own {@code JavaReflectionReferenceUtil.getReflectiveClass}).
 * <p>
 * Returns {@code null} whenever the target cannot be pinned to a concrete class at edit time — the
 * caller then contributes no reference, so a runtime-only class is simply left alone.
 */
final class TargetClassResolver {

    private TargetClassResolver() {
    }

    @Nullable
    static PsiClass resolve(RecognizedLookup lookup, PsiMethodCallExpression call) {
        switch (lookup.targetSource) {
            case CLASS_ARG0:
                return fromFirstArgument(call.getArgumentList());
            case RECEIVER_GENERIC:
                PsiExpression qualifier = call.getMethodExpression().getQualifierExpression();
                return qualifier == null ? null : fromClassReflectGeneric(qualifier.getType());
            default:
                return null;
        }
    }

    @Nullable
    private static PsiClass fromFirstArgument(PsiExpressionList arguments) {
        PsiExpression[] args = arguments.getExpressions();
        if (args.length == 0) {
            return null;
        }
        PsiExpression first = args[0];
        // The common case: a `SomeClass.class` literal.
        if (first instanceof PsiClassObjectAccessExpression) {
            PsiType operand = ((PsiClassObjectAccessExpression) first).getOperand().getType();
            return PsiUtil.resolveClassInClassTypeOnly(operand);
        }
        // A `Class<X>`-typed expression (e.g. a variable): extract X.
        return genericArgumentOf(first.getType(), "java.lang.Class");
    }

    @Nullable
    private static PsiClass fromClassReflectGeneric(@Nullable PsiType receiverType) {
        return genericArgumentOf(receiverType, RecognizedLookup.CLASS_REFLECT);
    }

    /**
     * @return the single type argument of {@code type} when {@code type} is {@code expectedRaw<X>}
     * and {@code X} resolves to a concrete class; otherwise {@code null}.
     */
    @Nullable
    private static PsiClass genericArgumentOf(@Nullable PsiType type, String expectedRawFqn) {
        if (!(type instanceof PsiClassType)) {
            return null;
        }
        PsiClassType classType = (PsiClassType) type;
        PsiClass raw = classType.resolve();
        if (raw == null || !expectedRawFqn.equals(raw.getQualifiedName())) {
            return null;
        }
        PsiType[] parameters = classType.getParameters();
        if (parameters.length != 1) {
            return null;
        }
        return PsiUtil.resolveClassInClassTypeOnly(parameters[0]);
    }
}
