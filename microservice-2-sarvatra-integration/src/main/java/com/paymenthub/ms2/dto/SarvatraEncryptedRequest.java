package com.paymenthub.ms2.dto;

public class SarvatraEncryptedRequest {
    private String ct;
    private String sk;
    private String iv;
    private String api;
    private String ts;

    // Constructors
    public SarvatraEncryptedRequest() {}

    public SarvatraEncryptedRequest(String ct, String sk, String iv, String api, String ts) {
        this.ct = ct;
        this.sk = sk;
        this.iv = iv;
        this.api = api;
        this.ts = ts;
    }

    // Getters
    public String getCt() { return ct; }
    public String getSk() { return sk; }
    public String getIv() { return iv; }
    public String getApi() { return api; }
    public String getTs() { return ts; }

    // Setters
    public void setCt(String ct) { this.ct = ct; }
    public void setSk(String sk) { this.sk = sk; }
    public void setIv(String iv) { this.iv = iv; }
    public void setApi(String api) { this.api = api; }
    public void setTs(String ts) { this.ts = ts; }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String ct;
        private String sk;
        private String iv;
        private String api;
        private String ts;

        public Builder ct(String ct) { this.ct = ct; return this; }
        public Builder sk(String sk) { this.sk = sk; return this; }
        public Builder iv(String iv) { this.iv = iv; return this; }
        public Builder api(String api) { this.api = api; return this; }
        public Builder ts(String ts) { this.ts = ts; return this; }

        public SarvatraEncryptedRequest build() {
            return new SarvatraEncryptedRequest(ct, sk, iv, api, ts);
        }
    }
}