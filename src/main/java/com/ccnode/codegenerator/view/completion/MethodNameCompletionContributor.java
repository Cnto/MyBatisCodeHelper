package com.ccnode.codegenerator.view.completion;

import com.ccnode.codegenerator.pojo.DomainClassInfo;
import com.ccnode.codegenerator.util.MethodNameUtil;
import com.ccnode.codegenerator.util.PsiClassUtil;
import com.ccnode.codegenerator.util.PsiElementUtil;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bruce.ge on 2016/12/8.
 */
public class MethodNameCompletionContributor extends CompletionContributor {
    private static List<String> textEndList = new ArrayList<String>() {{
        add("find");
        add("update");
        add("and");
        add("by");
        add("count");
    }};


    @Override
    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
        //todo maybe need to add the method to check.
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }
        PsiElement element = parameters.getPosition();
        PsiElement originalPosition = parameters.getOriginalPosition();
        PsiFile topLevelFile = InjectedLanguageUtil.getTopLevelFile(element);
        if (topLevelFile == null || !(topLevelFile instanceof PsiJavaFile)) {
            return;
        }
        PsiClass containingClass = PsiElementUtil.getContainingClass(originalPosition);
        if (containingClass == null || !containingClass.isInterface()) {
            return;
        }
        String text = originalPosition.getText();
        if (MethodNameUtil.checkValidTextStarter(text)) {
            //go tell them to choose.
            //todo could use like when there. why after press tab can't show with more?
//            get pojo class from it.
            DomainClassInfo domainClassInfo = PsiClassUtil.getDomainClassInfo(containingClass);
            PsiClass pojoClass = domainClassInfo.getDomainClass();
            if (pojoClass == null) {
                return;
            }
            List<String> strings = PsiClassUtil.extractProps(pojoClass);
            List<String> formatProps = new ArrayList<String>();
            for (String s : strings) {
                formatProps.add(s.substring(0, 1).toUpperCase() + s.substring(1));
            }
            String lower = text.toLowerCase();
            boolean defaultrecommed = false;
            //todo need fix when there is variable named 'by'
            for (String end : textEndList) {
                if (lower.endsWith(end)) {
                    defaultrecommed = true;
                    //add formated prop to recommend list.
                    for (String prop : formatProps) {
                        LookupElementBuilder builder = LookupElementBuilder.create(text + prop);
                        result.addElement(builder);
                    }
                }
                if (lower.equals("find")) {
                    result.addElement(LookupElementBuilder.create(text + "First"));
                    result.addElement(LookupElementBuilder.create(text + "One"));
                }
            }
            if (defaultrecommed) {
                return;
            }
            //todo may be can add more.
            List<String> afterlower = new ArrayList<String>();
            if(lower.indexOf("by")!=-1){
                if (lower.endsWith("g")) {
                    afterlower.add("reaterThan");
                    afterlower.add("reaterThanOrEqualTo");
                }
                if (lower.endsWith("l")) {
                    afterlower.add("essThan");
                    afterlower.add("essThanOrEqualTo");
                    afterlower.add("ike");
                }
                if (lower.endsWith("b")) {
                    afterlower.add("etween");
                    afterlower.add("etweenOrEqualTo");
                    afterlower.add("efore");
                }
                if (lower.endsWith("i")) {
                    afterlower.add("n");
                    afterlower.add("sNotNull");
                }
                if (lower.endsWith("n")) {
                    afterlower.add("otIn");
                    afterlower.add("otLike");
                    afterlower.add("ot");
                    afterlower.add("otNull");
                }
                if(lower.endsWith("o")){
                    afterlower.add("r");
                }

                if(lower.endsWith("a")){
                    afterlower.add("fter");
                }

                if(lower.endsWith("s")){
                    afterlower.add("tartingwith");
                }

                if(lower.endsWith("e")){
                    afterlower.add("ndingwith");
                }

                if(lower.endsWith("c")){
                    afterlower.add("ontaining");
                }

            }

            if(lower.endsWith("m")){
                afterlower.add("ax");
                afterlower.add("in");
            }

            if(lower.endsWith("a")){
                afterlower.add("vg");
                afterlower.add("nd");
            }

            if(lower.endsWith("s")){
                afterlower.add("um");
            }

            if (lower.equals("findd") || lower.equals("countd")) {
                afterlower.add("istinct");
            }

            if (lower.indexOf("orderby") != -1 && lower.endsWith("d")) {
                afterlower.add("esc");
            }
            if (lower.endsWith("o")) {
                afterlower.add("rderBy");
            }
            char u = Character.toLowerCase(text.charAt(text.length() - 1));

            for (String prop : strings) {
                if (Character.toLowerCase(prop.charAt(0)) == u && prop.length() > 1) {
                    afterlower.add(prop.substring(1));
                }
            }

            if (afterlower.size() > 0) {
                for (String after : afterlower) {
                    LookupElementBuilder builder = LookupElementBuilder.create(text + after);
                    result.addElement(builder);
                }
            }
        }
    }
}
