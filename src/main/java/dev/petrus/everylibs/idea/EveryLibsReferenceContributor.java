package dev.petrus.everylibs.idea;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * Registers reflection-aware references for EveryLibs lookups. Mirrors how IntelliJ's own
 * {@code JavaReflectionReferenceContributor} wires up completion/navigation for
 * {@code Class.getDeclaredField("…")}.
 * <p>
 * The pattern is intentionally broad — any string literal that is an argument of a method call —
 * with all the precise matching (declaring class, method, argument index, target resolution) done
 * in {@link EveryLibsMemberReferenceProvider}. This avoids depending on the exact
 * {@code PsiJavaPatterns.methodCallParameter} overloads, whose availability varies across platform
 * versions.
 */
public final class EveryLibsReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(PsiLiteralExpression.class)
                        .withParent(PlatformPatterns.psiElement(PsiExpressionList.class)),
                new EveryLibsMemberReferenceProvider());
    }
}
