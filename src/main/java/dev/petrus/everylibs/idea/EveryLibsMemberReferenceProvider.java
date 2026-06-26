package dev.petrus.everylibs.idea;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Turns a recognized lookup's name-string argument into an {@link EveryLibsMemberReference}.
 * <p>
 * Everything the broad pattern let through is verified here: the literal is a {@code String}; its
 * enclosing call resolves to a method that is in the {@link RecognizedLookup} registry; the literal
 * sits at that lookup's name-argument index; and the target class resolves to a concrete
 * {@link PsiClass}. Any failure contributes no reference, so non-matching overloads (e.g. the
 * {@code getField(String className, …)} form) and unresolved targets are ignored gracefully.
 */
public final class EveryLibsMemberReferenceProvider extends PsiReferenceProvider {

    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof PsiLiteralExpression)) {
            return PsiReference.EMPTY_ARRAY;
        }
        PsiLiteralExpression literal = (PsiLiteralExpression) element;
        if (!(literal.getValue() instanceof String)) {
            return PsiReference.EMPTY_ARRAY;
        }

        PsiElement argumentList = literal.getParent();
        if (!(argumentList instanceof PsiExpressionList)) {
            return PsiReference.EMPTY_ARRAY;
        }
        PsiElement callElement = argumentList.getParent();
        if (!(callElement instanceof PsiMethodCallExpression)) {
            return PsiReference.EMPTY_ARRAY;
        }
        PsiMethodCallExpression call = (PsiMethodCallExpression) callElement;

        PsiMethod method = call.resolveMethod();
        if (method == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        PsiClass declaring = method.getContainingClass();
        if (declaring == null || declaring.getQualifiedName() == null) {
            return PsiReference.EMPTY_ARRAY;
        }

        RecognizedLookup lookup = RecognizedLookup.find(declaring.getQualifiedName(), method.getName());
        if (lookup == null) {
            return PsiReference.EMPTY_ARRAY;
        }

        if (indexOf((PsiExpressionList) argumentList, literal) != lookup.nameArgIndex) {
            return PsiReference.EMPTY_ARRAY;
        }

        PsiClass target = TargetClassResolver.resolve(lookup, call);
        if (target == null) {
            return PsiReference.EMPTY_ARRAY;
        }

        return new PsiReference[]{new EveryLibsMemberReference(literal, target, lookup.memberKind)};
    }

    private static int indexOf(PsiExpressionList arguments, PsiLiteralExpression literal) {
        PsiExpression[] expressions = arguments.getExpressions();
        for (int i = 0; i < expressions.length; i++) {
            if (expressions[i] == literal) {
                return i;
            }
        }
        return -1;
    }
}
