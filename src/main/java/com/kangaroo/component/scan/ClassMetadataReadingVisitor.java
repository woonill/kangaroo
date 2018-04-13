package com.kangaroo.component.scan;

import com.kangaroo.util.CUtils;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Type;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;


public class ClassMetadataReadingVisitor extends ClassVisitor implements ClassMetadata {


    private String className;
    private boolean isInterface;
    private boolean isAbstract;
    private boolean isFinal;
    private String enclosingClassName;
    private boolean independentInnerClass;
    private String superClassName;
    private String[] interfaces;
    private Set<String> memberClassNames = new LinkedHashSet<String>();

    public ClassMetadataReadingVisitor(int api) {
        super(api);
    }

    public ClassMetadataReadingVisitor() {
        this(Opcodes.ASM4);
    }

    public void visit(int version, int access, String name, String signature, String supername, String[] interfaces) {
        this.className = CUtils.convertResourcePathToClassName(name);
        this.isInterface = ((access & Opcodes.ACC_INTERFACE) != 0);
        this.isAbstract = ((access & Opcodes.ACC_ABSTRACT) != 0);
        this.isFinal = ((access & Opcodes.ACC_FINAL) != 0);
        if (supername != null) {
            this.superClassName = CUtils.convertResourcePathToClassName(supername);
        }
        this.interfaces = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            this.interfaces[i] = CUtils.convertResourcePathToClassName(interfaces[i]);
        }
    }

    public void visitOuterClass(String owner, String name, String desc) {
        this.enclosingClassName = CUtils.convertResourcePathToClassName(owner);
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (outerName != null) {
            String fqName = CUtils.convertResourcePathToClassName(name);
            String fqOuterName = CUtils.convertResourcePathToClassName(outerName);
            if (this.className.equals(fqName)) {
                this.enclosingClassName = fqOuterName;
                this.independentInnerClass = ((access & Opcodes.ACC_STATIC) != 0);
            } else if (this.className.equals(fqOuterName)) {
                this.memberClassNames.add(fqName);
            }
        }

    }

    private Set<AnnotationMetadata> annotationSet = new HashSet<AnnotationMetadata>();

    private class DefaultAnnotationVisitor extends AnnotationVisitor {

        private AnnotationMetadata aMetadata;

        public DefaultAnnotationVisitor(int api, AnnotationMetadata annotationMetadata) {
            super(api);
            this.aMetadata = annotationMetadata;
        }

        @Override
        public void visit(String name, Object value) {
            this.aMetadata.setValue(name, value);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        String className = Type.getType(desc).getClassName();
        AnnotationMetadata ams = new AnnotationMetadata(this.getClassName(), className);
        this.annotationSet.add(ams);
        return new DefaultAnnotationVisitor(this.api, ams);
    }

    public String getClassName() {
        return this.className;
    }

    public boolean isInterface() {
        return this.isInterface;
    }

    public boolean isAbstract() {
        return this.isAbstract;
    }

    public boolean isConcrete() {
        return !(this.isInterface || this.isAbstract);
    }

    public boolean isFinal() {
        return this.isFinal;
    }

    public boolean isIndependent() {
        return (this.enclosingClassName == null || this.independentInnerClass);
    }

    public boolean hasEnclosingClass() {
        return (this.enclosingClassName != null);
    }

    public String getEnclosingClassName() {
        return this.enclosingClassName;
    }

    public boolean hasSuperClass() {
        return (this.superClassName != null);
    }

    public String getSuperClassName() {
        return this.superClassName;
    }

    public String[] getInterfaceNames() {
        return this.interfaces;
    }

    public String[] getMemberClassNames() {
        return this.memberClassNames.toArray(new String[this.memberClassNames.size()]);
    }

    @Override
    public AnnotationMetadata[] getAnnotations() {
        Set<AnnotationMetadata> anns = annotationSet;
        return anns.toArray(new AnnotationMetadata[anns.size()]);
    }
}
