package dev.petrus.everylibs.idea;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A reference from a member-name string literal to the {@link PsiField}/{@link PsiMethod} it names
 * on the resolved target class. Modelled on IntelliJ's {@code JavaLangClassMemberReference}.
 * <p>
 * {@code getRangeInElement()} is deliberately <b>not</b> overridden: {@link PsiReferenceBase}
 * derives the inside-the-quotes range from the platform's {@code StringLiteralManipulator}, which
 * also handles Rename writing back into the literal — so no element manipulator is registered here.
 * The reference is <b>soft</b>: whether a name resolves is ultimately a runtime concern, so an
 * unknown name is not flagged as a hard error.
 */
final class EveryLibsMemberReference extends PsiReferenceBase<PsiLiteralExpression> {

    private final PsiClass target;
    private final MemberKind kind;

    EveryLibsMemberReference(PsiLiteralExpression literal, PsiClass target, MemberKind kind) {
        super(literal, true);
        this.target = target;
        this.kind = kind;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        String name = nameValue();
        if (name == null) {
            return null;
        }
        if (kind == MemberKind.FIELD) {
            for (PsiField field : MemberWalk.fields(target)) {
                if (name.equals(field.getName())) {
                    return field;
                }
            }
        } else {
            for (PsiMethod method : MemberWalk.methods(target)) {
                if (name.equals(method.getName())) {
                    return method;
                }
            }
        }
        return null;
    }

    @Override
    public Object[] getVariants() {
        List<LookupElement> variants = new ArrayList<>();
        if (kind == MemberKind.FIELD) {
            for (PsiField field : MemberWalk.fields(target)) {
                if (isFromObject(field)) {
                    continue;
                }
                variants.add(LookupElementBuilder.create(field.getName())
                        .withIcon(field.getIcon(0))
                        .withTypeText(field.getType().getPresentableText()));
            }
        } else {
            for (PsiMethod method : MemberWalk.methods(target)) {
                if (isFromObject(method)) {
                    continue;
                }
                variants.add(LookupElementBuilder.create(method.getName())
                        .withIcon(method.getIcon(0))
                        .withTailText(parameterText(method), true)
                        .withTypeText(returnTypeText(method)));
            }
        }
        return variants.toArray();
    }

    /**
     * Suggestion-only filter: a member declared purely on {@link Object} (an un-overridden
     * {@code toString}/{@code wait}/…) is noise in the popup. {@link #resolve()} stays faithful — it
     * still resolves such a name if the developer types it — so navigation/rename are unaffected.
     */
    private static boolean isFromObject(PsiMember member) {
        PsiClass owner = member.getContainingClass();
        return owner != null && CommonClassNames.JAVA_LANG_OBJECT.equals(owner.getQualifiedName());
    }

    @Nullable
    private String nameValue() {
        Object value = getElement().getValue();
        return value instanceof String ? (String) value : null;
    }

    private static String parameterText(PsiMethod method) {
        StringBuilder text = new StringBuilder("(");
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(parameters[i].getType().getPresentableText());
        }
        return text.append(")").toString();
    }

    private static String returnTypeText(PsiMethod method) {
        PsiType returnType = method.getReturnType();
        return returnType == null ? "" : returnType.getPresentableText();
    }
}
