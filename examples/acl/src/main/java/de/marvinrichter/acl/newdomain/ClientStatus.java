package de.marvinrichter.acl.newdomain;

public enum ClientStatus {
    INACTIVE, ACTIVE, SUSPENDED;

    public static ClientStatus fromLegacyCode(int code) {
        return switch (code) {
            case 0 -> INACTIVE;
            case 1 -> ACTIVE;
            case 2 -> SUSPENDED;
            default -> throw new IllegalArgumentException(
                    "Unknown legacy status code: " + code);
        };
    }

    public int toLegacyCode() {
        return this.ordinal();
    }
}
