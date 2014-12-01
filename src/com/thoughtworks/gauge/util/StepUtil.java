package com.thoughtworks.gauge.util;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.util.Query;
import com.thoughtworks.gauge.ConceptInfo;
import com.thoughtworks.gauge.GaugeConnection;
import com.thoughtworks.gauge.StepValue;
import com.thoughtworks.gauge.core.Gauge;
import com.thoughtworks.gauge.core.GaugeService;
import com.thoughtworks.gauge.language.psi.SpecStep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.thoughtworks.gauge.GaugeConstant.STEP_ANNOTATION_QUALIFIER;

public class StepUtil {

    public static PsiMethod findStepImpl(SpecStep step, Project project) {
        Collection<PsiMethod> stepMethods = getStepMethods(project);
        return filter(stepMethods, step);
    }

    private static PsiMethod filter(Collection<PsiMethod> stepMethods, SpecStep step) {
        String stepText = step.getStepValue().getStepText();
        for (PsiMethod stepMethod : stepMethods) {
            if (isMatch(stepMethod, stepText)) {
                return stepMethod;
            }
        }
        return null;
    }

    public static boolean isMatch(PsiMethod stepMethod, String stepText) {
        final PsiModifierList modifierList = stepMethod.getModifierList();
        final PsiAnnotation[] annotations = modifierList.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (annotationTextMatches(annotation, stepText)) {
                return true;
            }
        }
        return false;
    }

    private static boolean annotationTextMatches(PsiAnnotation annotation, String stepValue) {
        Module moduleForPsiElement = ModuleUtil.findModuleForPsiElement(annotation);
        GaugeService gaugeService = Gauge.getGaugeService(moduleForPsiElement);
        if (gaugeService != null) {
            String annotationValue = AnnotationUtil.getStringAttributeValue(annotation, "value");
            GaugeConnection gaugeConnection = gaugeService.getGaugeConnection();
            String methodValue = gaugeConnection.getStepValue(annotationValue).getStepText();
            return methodValue.equals(stepValue);
        }
        return false;
    }

    public static Collection<PsiMethod> getStepMethods(Project project) {
        final PsiClass step = JavaPsiFacade.getInstance(project).findClass(STEP_ANNOTATION_QUALIFIER, GlobalSearchScope.allScope(project));
        if (step != null) {
            final Query<PsiMethod> psiMethods = AnnotatedElementsSearch.searchPsiMethods(step, GlobalSearchScope.allScope(project));
            return psiMethods.findAll();
        }
        return new ArrayList<PsiMethod>();
    }

    public static boolean isStepImplementation(PsiElement element) {
        if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;
            PsiModifierList modifierList = method.getModifierList();
            PsiAnnotation[] annotations = modifierList.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                if (STEP_ANNOTATION_QUALIFIER.equals(annotation.getQualifiedName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getMethodAnnotationText(PsiElement element) {
        if (element instanceof PsiMethod) {
            PsiMethod method = (PsiMethod) element;
            PsiModifierList modifierList = method.getModifierList();
            PsiAnnotation[] annotations = modifierList.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                if (STEP_ANNOTATION_QUALIFIER.equals(annotation.getQualifiedName())) {
                    String attributeValue = AnnotationUtil.getStringAttributeValue(annotation, "value");
                    Module moduleForElement = ModuleUtil.findModuleForPsiElement(element);
                    GaugeService gaugeService = Gauge.getGaugeService(moduleForElement);
                    if (gaugeService != null) {
                        return gaugeService.getGaugeConnection().getStepValue(attributeValue).getStepText();
                    } else {
                        return attributeValue;
                    }
                }
            }
        }
        return "";
    }


    public static boolean isImplementedStep(SpecStep step, Project project) {
        if (isConcept(step)) {
            return true;
        } else {
            return findStepImpl(step, project) != null;
        }
    }

    //Check if the step is a concept using list of concepts got from gauge API
    private static boolean isConcept(SpecStep step) {
            try {
                Module module = ModuleUtil.findModuleForPsiElement(step);
                GaugeService gaugeService = Gauge.getGaugeService(module);
                if (gaugeService != null) {
                    List<ConceptInfo> conceptInfos = gaugeService.getGaugeConnection().fetchAllConcepts();
                    return conceptExists(conceptInfos, step.getStepValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        return false;
        }

    private static boolean conceptExists(List<ConceptInfo> conceptInfos, StepValue stepValue) {
        for (ConceptInfo conceptInfo : conceptInfos) {
            if (conceptInfo.getStepValue().getStepText().trim().equals(stepValue.getStepText().trim())) {
                return true;
            }
        }
        return false;
    }
}