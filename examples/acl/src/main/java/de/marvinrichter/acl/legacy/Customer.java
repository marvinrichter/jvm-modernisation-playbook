package de.marvinrichter.acl.legacy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Legacy data model — abbreviated field names, numeric status, date as string.
 * We do not own this schema; it cannot be renamed.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    public String custId;
    public String custNm;     // customer name
    public String custAddr1;  // address line 1
    public String custAddr2;  // address line 2 (may be null)
    public int    status;     // 0=inactive, 1=active, 2=suspended
    public String createDt;   // date as "yyyyMMdd" string

    protected Customer() {}

    public Customer(String custId, String custNm, String custAddr1,
                    String custAddr2, int status, String createDt) {
        this.custId   = custId;
        this.custNm   = custNm;
        this.custAddr1 = custAddr1;
        this.custAddr2 = custAddr2;
        this.status   = status;
        this.createDt = createDt;
    }
}
