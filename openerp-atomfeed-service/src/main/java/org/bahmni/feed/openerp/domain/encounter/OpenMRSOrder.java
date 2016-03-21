package org.bahmni.feed.openerp.domain.encounter;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMRSOrder {
    private OpenMRSConcept concept;
    private String action;
    private String uuid;
    private String orderType;
    private String orderNumber;
    private String previousOrderUuid;
    private String commentToFulfiller;
    private boolean voided;

    public OpenMRSConcept getConcept() {
        return concept;
    }

    public String getAction() {
        return action;
    }

    public String getUuid() {
        return uuid;
    }

    public String getOrderType() {
        return orderType;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getPreviousOrderUuid() {
        return previousOrderUuid;
    }

    public String getCommentToFulfiller() {
        return commentToFulfiller;
    }

    public boolean isVoided() {
        return voided;
    }

    public String getConceptUuid() {
        return getConcept() != null ? getConcept().getUuid() : null;
    }

    public String getConceptName() {
        return getConcept() != null ? getConcept().getName() : null;
    }

    public String getConceptClass() {
        return getConcept() != null ? getConcept().getConceptClass() : null;
    }

}
