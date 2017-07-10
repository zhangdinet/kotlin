package test;

public class CompanionObjectVisibilityFromJava {
    public static Object test0 = C0.PublicCO;

    public static Object test1 = C1.PrivateCO;

    public static Object test2 = C2.ProtectedCO;

    public static class C2Derived extends C2 {
        public static Object test2a = C2.ProtectedCO;
    }
}
