-basedirectory build\libs

-injars Server.jar
-injars lib\InfotainmentLib5.jar
-outjars ServerObfuscated.jar

-libraryjars lib(!**InfotainmentLib5.jar;)
-libraryjars <java.home>/lib/jce.jar

-printmapping a.txt
-keepparameternames #TODO: necessary?

-dontshrink
-dontoptimize
-useuniqueclassmembernames
-keepdirectories
-keeppackagenames
-adaptclassstrings
-dontusemixedcaseclassnames
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,LocalVariable*Table,*Annotation*,Synthetic,EnclosingMethod

# Main methods
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# Data for server UI
-keep public class **.frontend.model.** {
    public <fields>;
    public <methods>;
}
