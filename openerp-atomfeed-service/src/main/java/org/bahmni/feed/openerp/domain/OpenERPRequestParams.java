package org.bahmni.feed.openerp.domain;

import org.bahmni.feed.openerp.ObjectMapperRepository;
import org.bahmni.feed.openerp.OpenMRSEncounterParser;
import org.bahmni.feed.openerp.domain.encounter.OpenERPOrder;
import org.bahmni.feed.openerp.domain.encounter.OpenERPOrders;
import org.bahmni.feed.openerp.domain.encounter.OpenMRSEncounter;
import org.bahmni.feed.openerp.domain.encounter.OpenMRSOrder;
import org.bahmni.feed.openerp.domain.visit.OpenMRSVisit;
import org.bahmni.openerp.web.request.OpenERPRequest;
import org.bahmni.openerp.web.request.builder.Parameter;
import org.bahmni.openerp.web.service.ProductService;
import org.codehaus.jackson.map.ObjectMapper;
import org.ict4h.atomfeed.client.domain.Event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OpenERPRequestParams {

    public static final String ADMISSION_CHARGES = "Admission Charges";
    private Event event;
    private String feedUrl;
    public static ObjectMapper objectMapper = new ObjectMapper();
    private ProductService productService;

    public OpenERPRequestParams(Event event, String feedUrl, ProductService productService) {
        this.event = event;
        this.feedUrl = feedUrl;
        this.productService = productService;
    }

    private List<Parameter> getParameters(OpenMRSEncounter openMRSEncounter) throws IOException {
        String feedUri = event.getFeedUri();
        List<Parameter> parameters = new ArrayList<>();
        String patientDisplay = openMRSEncounter.getPatient().getDisplay();
        String patientId = patientDisplay.split(" ")[0];
        validateUrls(feedUri);
        parameters.add(createParameter("category", "create.sale.order", "string"));
        parameters.add(createParameter("customer_id", patientId, "string"));
        parameters.add(createParameter("feed_uri", feedUrl, "string"));
        parameters.add(createParameter("last_read_entry_id", event.getId(), "string"));
        parameters.add(createParameter("feed_uri_for_last_read_entry", feedUri, "string"));

        OpenERPOrders orders = new OpenERPOrders();
        orders.setId(openMRSEncounter.getUuid());

        mapOrders(openMRSEncounter, parameters, orders);
        return parameters;
    }

    private OpenMRSEncounter getOpenMRSEncounter(String orderJSON) throws IOException {
        OpenMRSEncounterParser openMRSEncounterParser = new OpenMRSEncounterParser(ObjectMapperRepository.objectMapper);
        return openMRSEncounterParser.parse(orderJSON);
    }

    private void validateUrls(String feedUri) {
        if((feedUrl != null && feedUrl.contains("$param")) || (feedUri != null && feedUri.contains("$param")))
            throw new RuntimeException("Junk values in the feedUrl:$param**");
    }

    private void mapOrders(OpenMRSEncounter openMRSEncounter, List<Parameter> parameters, OpenERPOrders orders) throws IOException {
        OpenMRSVisit visit = openMRSEncounter.getVisit();
        if (hasOrders(openMRSEncounter)){
            for(OpenMRSOrder order : openMRSEncounter.getOrders()) {
                addNewOrders(orders, visit, order);
            }
        }else if(isAdmissionEncounter(openMRSEncounter)){
            addAdmissionChargeOrder(orders, visit);
        }
        String ordersJson = objectMapper.writeValueAsString(orders);
        parameters.add(createParameter("orders", ordersJson, "string"));
    }

    private void addAdmissionChargeOrder(OpenERPOrders orders, OpenMRSVisit visit) {
        OpenERPOrder openERPOrder = new OpenERPOrder();
        setVisitDetails(openERPOrder,visit);
        List<String> productIds = new ArrayList<>();
        String productId = productService.findProductByName(ADMISSION_CHARGES);
        if(productId != null)
            productIds.add(productId);
        openERPOrder.setProductIds(productIds);
        orders.getOpenERPOrders().add(openERPOrder);
    }

    private void addNewOrders(OpenERPOrders orders, OpenMRSVisit visit, OpenMRSOrder order) {
        OpenERPOrder openERPOrder = new OpenERPOrder();
        openERPOrder.setId(order.getUuid());
        setVisitDetails(openERPOrder,visit);
        List<String> productIds = new ArrayList<>();
        productIds.add(order.getConcept().getUuid());
        openERPOrder.setProductIds(productIds);
        orders.getOpenERPOrders().add(openERPOrder);
    }

    private boolean isAdmissionEncounter(OpenMRSEncounter openMRSEncounter) {
        return openMRSEncounter.getEncounterType().getName().equals(OpenMRSEncounter.TYPE_ADMISSION);
    }

    private boolean hasOrders(OpenMRSEncounter openMRSEncounter) {
        return openMRSEncounter.getOrders().size() > 0;
    }

    private void setVisitDetails(OpenERPOrder openERPOrder, OpenMRSVisit visit) {
        openERPOrder.setVisitId(visit.getUuid());
        openERPOrder.setVisitType(visit.getVisitType());
        openERPOrder.setDescription(visit.getDescription());
    }

    private Parameter createParameter(String name, String value, String type) {
        return new Parameter(name, value, type);
    }

    public OpenERPRequest getRequest(String encounterEventContent) throws IOException {
        OpenMRSEncounter openMRSEncounter = getOpenMRSEncounter(encounterEventContent);

        if (!openMRSEncounter.shouldERPConsumeEvent()) {
            return OpenERPRequest.DO_NOT_CONSUME;
        }

        return new OpenERPRequest("atom.event.worker", "process_event", getParameters(openMRSEncounter));
    }
}
