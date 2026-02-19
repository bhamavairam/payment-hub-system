package com.paymenthub.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    // ISO 8583 fields as a plain map
    // Key = field number as string ("0", "2", "3" ... "128")
    // Value = field value as string
    // Example:
    //   "0"  → "0200"
    //   "2"  → "411111bankiin0uid"
    //   "3"  → "000000"
    //   "4"  → "000000002050"
    //   "11" → "123456"  ← this is STAN, used as correlationId
    //   "41" → "TERM001"
    private Map<String, String> fields;

    // Helper: get field 11 (STAN) as correlationId
    public String getStan() {
        return fields != null ? fields.get("11") : null;
    }

    // Helper: get field 41 (Terminal ID)
    public String getTerminalId() {
        return fields != null ? fields.get("41") : null;
    }

    // Helper: get field 0 (MTI)
    public String getMti() {
        return fields != null ? fields.get("0") : null;
    }
}