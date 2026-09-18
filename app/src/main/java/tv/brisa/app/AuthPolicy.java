package tv.brisa.app;
final class AuthPolicy {
    static int nextInterval(int current, String error) { return "slow_down".equals(error) ? current + 5 : current; }
    static boolean pending(String error) { return "authorization_pending".equals(error) || "slow_down".equals(error); }
    static boolean validClient(String value) { return value != null && value.matches("[a-zA-Z0-9]{20,64}"); }
}
