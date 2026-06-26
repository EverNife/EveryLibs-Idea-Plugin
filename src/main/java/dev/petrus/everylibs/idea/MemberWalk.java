package dev.petrus.everylibs.idea;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * The PSI twin of the library's {@code TypeHierarchy}: the members a EveryLibs lookup would actually
 * resolve at runtime.
 * <p>
 * It walks the superclass chain using <em>declared</em> members (so inherited {@code private}
 * members show up — the whole point of reflecting visible-but-inaccessible state), then the
 * implemented interfaces for their constants and un-overridden {@code default} methods. The class
 * tier is emitted before the interface tier so a class/superclass member shadows an inherited
 * default; a nearer interface wins over a farther one. Members are de-duplicated by name (the first,
 * nearest declaration wins) since completion offers each name once.
 */
final class MemberWalk {

    private MemberWalk() {
    }

    static List<PsiField> fields(PsiClass target) {
        LinkedHashMap<String, PsiField> byName = new LinkedHashMap<>();
        for (PsiClass type = target; type != null; type = type.getSuperClass()) {
            for (PsiField field : type.getFields()) {
                byName.putIfAbsent(field.getName(), field);
            }
        }
        for (PsiClass itf : interfaces(target)) {
            for (PsiField constant : itf.getFields()) {
                byName.putIfAbsent(constant.getName(), constant);
            }
        }
        return new ArrayList<>(byName.values());
    }

    static List<PsiMethod> methods(PsiClass target) {
        LinkedHashMap<String, PsiMethod> byName = new LinkedHashMap<>();
        for (PsiClass type = target; type != null; type = type.getSuperClass()) {
            for (PsiMethod method : type.getMethods()) {
                byName.putIfAbsent(method.getName(), method);
            }
        }
        for (PsiClass itf : interfaces(target)) {
            for (PsiMethod method : itf.getMethods()) {
                // Only concrete default methods are inherited; static interface methods are not.
                if (method.hasModifierProperty(PsiModifier.DEFAULT)) {
                    byName.putIfAbsent(method.getName(), method);
                }
            }
        }
        return new ArrayList<>(byName.values());
    }

    /** Every interface implemented anywhere on {@code target}'s class chain, breadth-first. */
    private static List<PsiClass> interfaces(PsiClass target) {
        List<PsiClass> order = new ArrayList<>();
        Set<PsiClass> seen = new HashSet<>();
        Deque<PsiClass> queue = new ArrayDeque<>();
        for (PsiClass type = target; type != null; type = type.getSuperClass()) {
            enqueue(type.getInterfaces(), order, seen, queue);
        }
        while (!queue.isEmpty()) {
            enqueue(queue.poll().getInterfaces(), order, seen, queue);
        }
        return order;
    }

    private static void enqueue(PsiClass[] interfaces, List<PsiClass> order,
                                Set<PsiClass> seen, Deque<PsiClass> queue) {
        for (PsiClass itf : interfaces) {
            if (itf != null && seen.add(itf)) {
                order.add(itf);
                queue.add(itf);
            }
        }
    }
}
