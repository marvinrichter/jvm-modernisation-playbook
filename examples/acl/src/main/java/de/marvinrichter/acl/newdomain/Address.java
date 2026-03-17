package de.marvinrichter.acl.newdomain;

public record Address(String line1, String line2) {
    public Address(String line1) {
        this(line1, null);
    }
}
