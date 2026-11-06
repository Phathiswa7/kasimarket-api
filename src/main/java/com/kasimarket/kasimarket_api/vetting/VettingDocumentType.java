package com.kasimarket.kasimarket_api.vetting;

// The kinds of document a worker can upload.
public enum VettingDocumentType {
    ID_DOCUMENT,        // identity document, such as a South African ID
    POLICE_CLEARANCE,   // criminal record clearance certificate
    REFERENCE_LETTER    // a reference (needed later for tier 3)
}