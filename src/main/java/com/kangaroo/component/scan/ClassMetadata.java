package com.kangaroo.component.scan;


public interface ClassMetadata {


    public String getClassName();

    public boolean isInterface();

    public boolean isAbstract();

    public boolean isConcrete();

    public boolean isFinal();

    public boolean isIndependent();

    public boolean hasEnclosingClass();

    public String getEnclosingClassName();

    public boolean hasSuperClass();

    public String getSuperClassName();

    public String[] getInterfaceNames();

    public String[] getMemberClassNames();

    public AnnotationMetadata[] getAnnotations();
}
