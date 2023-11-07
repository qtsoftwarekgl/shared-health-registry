package rhies.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.mongodb.*;
import com.mongodb.util.JSON;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.UnknownHostException;
import java.util.*;

public class rhies_EncounterResourceProvider implements IResourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(rhies_EncounterResourceProvider.class);

    FhirContext ctx = FhirContext.forR4();
    IParser parser = ctx.newJsonParser();

    public rhies_EncounterResourceProvider() {
        try {
            DBCollection userCollection = CommonDbConnection.dbConnection().getCollection("users");
            BasicDBObject user = new BasicDBObject("username", "rhiesEMR").append("password", "YWRtaW5QYXNzMTIzNA==");
            DBCursor cursor = userCollection.find(user);
            DBObject dbobject = cursor.one();
            if (dbobject == null) {
                userCollection.insert(user);
            }

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Encounter.class;
    }

    /**
     * Implementation of the "read" (Get) method
     *
     * @throws IOException
     * @throws SecurityException
     */
    @Read
    public Encounter read(@IdParam IdType theId) throws ResourceNotFoundException, SecurityException, IOException {
        Encounter fhirPatient = new Encounter();

        DBCollection encounterCollection = CommonDbConnection.dbConnection().getCollection("encounter");

        BasicDBObject query = new BasicDBObject("id", theId.getValue().split("/")[1]);
        DBCursor cursor = encounterCollection.find(query);
        DBObject dbobject = cursor.one();
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setAction(AuditEvent.AuditEventAction.R);
        auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("vread").setDisplay("search"));
        auditEvent.setRecorded(new Date());


        if (dbobject != null) {
            String patientData = dbobject.toString();
            fhirPatient = parser.parseResource(Encounter.class, patientData);
            auditEvent.setOutcomeDesc("Encounter search based on id: " + theId);
            rhies_FHIRBALPInterceptor.storeAuditEvent(auditEvent);
        } else {
            auditEvent.setOutcomeDesc("Encounter search based on id: " + theId + " not found!");
            rhies_FHIRBALPInterceptor.storeAuditEvent(auditEvent);
            utils.error("Encounter " + theId + " not found!");
        }

        return fhirPatient;
    }

    /**
     * Implementation of the "delete" (Get) method
     *
     * @throws UnknownHostException
     */
    @Delete
    public MethodOutcome delete(@IdParam IdType theId) throws UnknownHostException, ResourceNotFoundException {
        MethodOutcome method = new MethodOutcome();

        DBCollection patientCollection = CommonDbConnection.dbConnection().getCollection("encounter");
        BasicDBObject query = new BasicDBObject("id", theId.getValue().split("/")[1]);
        patientCollection.remove(query);

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setAction(AuditEvent.AuditEventAction.D);
        auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("delete").setDisplay("delete"));
        auditEvent.setRecorded(new Date());
        auditEvent.setOutcomeDesc(" Encounter deleted: " + theId.getValue().toString());

        rhies_FHIRBALPInterceptor.storeAuditEvent(auditEvent);

        method.setCreated(true);

        return method;
    }

    @Search
    public List<Encounter> search(
            @OptionalParam(name = "searchSet") StringParam searchSet,
            @OptionalParam(name = "value") StringParam value,
            @OptionalParam(name = "page") StringParam page,
            @OptionalParam(name = "size") StringParam size,
            @OptionalParam(name = "fromDate") String fromDate,
            @OptionalParam(name = "toDate") String toDate,
            @OptionalParam(name = "type") String type,
            @OptionalParam(name = "location") String location
    ) throws UnknownHostException {
        BasicDBObject query = new BasicDBObject();


        //{"subject.display":{$regex : "0990980809976778798"}}


        if (searchSet != null) {
            if (searchSet.getValue().equalsIgnoreCase(Constants.SEARCH_SET_ALL)) {
                if (value != null) {
                    query.append("subject.identifier.value", buildSearchPattern(value.getValue()));
                    List<String> exclude = new ArrayList<String>();
                    exclude.add("Drug Order Encounter");
                    exclude.add("LAB TEST");
                    exclude.add("LAB TEST - VLSM");
                    exclude.add("LAB TEST - NRL");
                    query.append("type.coding.display", new BasicDBObject("$nin", exclude));
                    BasicDBList secondOrValues = new BasicDBList();
                    if (fromDate != null && !fromDate.isEmpty() && toDate != null && !toDate.isEmpty()) {
                        //  DBObject batchId = new BasicDBObject( "period.start", new BasicDBObject("$gte", fromDate));
                        //secondOrValues.add( batchId );
                        //  query.append("period.start", new BasicDBObject("$gte", fromDate));

                        query.append("period.start", BasicDBObjectBuilder.start("$gte", fromDate).add("$lte", toDate).get());

                    }

                    if (fromDate != null && !fromDate.isEmpty() && (toDate == null || toDate.isEmpty())) {
                        query.append("period.start", new BasicDBObject("$gte", fromDate));
                    }

                    if ((fromDate == null || fromDate.isEmpty()) && toDate != null && !toDate.isEmpty()) {
                        query.append("period.start", new BasicDBObject("$lte", toDate));
                    }
                   /* if(toDate != null && !toDate.isEmpty()){
                        query.append("period.start", new BasicDBObject("$lte", toDate));
                       *//* DBObject batchId = new BasicDBObject( "period.start", new BasicDBObject("$lte", toDate));
                        secondOrValues.add( batchId );*//*
                    }*/

                    if (type != null && !type.isEmpty()) {
                        query.append("type.coding.display", buildSearchPattern(type));
                        /*DBObject batchId = new BasicDBObject( "type.coding.display", buildSearchPattern(type));
                        secondOrValues.add( batchId );*/
                    }

                    if (location != null && !location.isEmpty()) {
                        DBObject batchId = new BasicDBObject("location.location.display", buildSearchPattern(location));
                        DBObject fileName = new BasicDBObject("location.location.identifier.value", buildSearchPattern(location));
                        secondOrValues.add(batchId);
                        secondOrValues.add(fileName);
                    }
                    if (!secondOrValues.isEmpty()) {
                        BasicDBObject dbObject = new BasicDBObject();
                        query.append("$or", secondOrValues);
                        //query.append("$and", dbObject);
                    }
                    LOGGER.error(new Gson().toJson(query));
                }else{
                    if (fromDate != null && !fromDate.isEmpty() && toDate != null && !toDate.isEmpty()) {
                        query.append("period.start", BasicDBObjectBuilder.start("$gte", fromDate).add("$lte", toDate).get());
                    }
                    BasicDBList secondOrValues = new BasicDBList();
                    if (location != null && !location.isEmpty()) {
                        DBObject batchId = new BasicDBObject("location.location.display", buildSearchPattern(location));
                        DBObject fileName = new BasicDBObject("location.location.identifier.value", buildSearchPattern(location));
                        secondOrValues.add(batchId);
                        secondOrValues.add(fileName);
                    }
                    if (!secondOrValues.isEmpty()) {
                        query.append("$or", secondOrValues);
                    }
                    LOGGER.error(new Gson().toJson(query));
                }
            } else if (searchSet.getValue().equalsIgnoreCase(Constants.SEARCH_SET_OTHERS)) {
                if (value != null) {
                    query.append("subject.identifier.value", buildSearchPattern(value.getValue()));
                    List<String> exclude = new ArrayList<String>();
                    exclude.add("Registration");
                    exclude.add("Drug Order Encounter");
                    exclude.add("LAB TEST");
                    exclude.add("Drug administration");
                    exclude.add("Diagnosis");
                    query.append("type.coding.display", new BasicDBObject("$nin", exclude));
                    LOGGER.error(new Gson().toJson(query));
                }
            } else if (searchSet.getValue().equalsIgnoreCase(Constants.SEARCH_SET_REGISTRATION)) {
                if (value != null) {
                    query.append("subject.identifier.value", buildSearchPattern(value.getValue()));
                    List<String> exclude = new ArrayList<String>();
                    exclude.add("Registration");
                    query.append("type.coding.display", new BasicDBObject("$in", exclude));
                    LOGGER.error(new Gson().toJson(query));
                }
            } else if (searchSet.getValue().equalsIgnoreCase(Constants.SEARCH_SET_DRUG_ORDER)) {
                if (value != null) {
                    query.append("subject.identifier.value", buildSearchPattern(value.getValue()));
                    List<String> exclude = new ArrayList<String>();
                    exclude.add("Drug Order Encounter");
                    query.append("type.coding.display", new BasicDBObject("$in", exclude));
                    LOGGER.error(new Gson().toJson(query));
                }
            } else if (searchSet.getValue().equalsIgnoreCase(Constants.SEARCH_SET_LAB_TEST)) {
                if (value != null) {
                    query.append("subject.identifier.value", buildSearchPattern(value.getValue()));
                    List<String> exclude = new ArrayList<String>();
                    exclude.add("LAB TEST");
                    query.append("type.coding.display", new BasicDBObject("$in", exclude));
                    LOGGER.error(new Gson().toJson(query));
                }
            } else if (searchSet.getValue().equalsIgnoreCase(Constants.SEARCH_SET_DRUG_ADMINISTRATION)) {
                if (value != null) {
                    query.append("subject.identifier.value", buildSearchPattern(value.getValue()));
                    List<String> exclude = new ArrayList<String>();
                    exclude.add("Drug administration");
                    query.append("type.coding.display", new BasicDBObject("$in", exclude));
                    LOGGER.error(new Gson().toJson(query));
                }
            } else if (searchSet.getValue().equalsIgnoreCase(Constants.SEARCH_SET_DIAGNOSIS)) {
                if (value != null) {
                    query.append("subject.identifier.value", buildSearchPattern(value.getValue()));
                    List<String> exclude = new ArrayList<String>();
                    exclude.add("Diagnosis");
                    query.append("type.coding.display", new BasicDBObject("$in", exclude));
                    LOGGER.error(new Gson().toJson(query));
                }
            }
        }

        if (fromDate != null && !fromDate.isEmpty() && toDate != null && !toDate.isEmpty()) {
            query.append("period.start", BasicDBObjectBuilder.start("$gte", fromDate).add("$lte", toDate).get());
        }
        BasicDBList secondOrValues = new BasicDBList();
        if (location != null && !location.isEmpty()) {
            DBObject batchId = new BasicDBObject("location.location.display", buildSearchPattern(location));
            DBObject fileName = new BasicDBObject("location.location.identifier.value", buildSearchPattern(location));
            secondOrValues.add(batchId);
            secondOrValues.add(fileName);
        }
        if (!secondOrValues.isEmpty()) {
            query.append("$or", secondOrValues);
        }
        LOGGER.error(new Gson().toJson(query));
        DBCollection patientCollection = CommonDbConnection.dbConnection().getCollection("encounter");
        List<Encounter> retVal = new ArrayList<Encounter>();
//        DBCursor cursor = patientCollection.aggregate();


        String[] logicalId = null;
        long recordCount = patientCollection.count(query);

        Integer pageNumber = 1;
        if (page != null) {
            pageNumber = Integer.parseInt(page.getValue());
            if (pageNumber == 0) {
                pageNumber = 1;
            }
        }
        Integer nPerPage = 10;
        if (size != null) {
            nPerPage = Integer.parseInt(size.getValue());
            if (nPerPage == 0) {
                nPerPage = 10;
            }
        }

        DBCursor cursor = patientCollection.find(query).sort(new BasicDBObject("period.start", -1)).skip(pageNumber > 0 ? ((pageNumber - 1) * nPerPage) : 0)
                .limit(nPerPage);
        for (Iterator iterator = cursor.iterator(); iterator.hasNext(); ) {
            Object next = iterator.next();
            String patientData = next.toString();
            Encounter patient = parser.parseResource(Encounter.class, patientData);
            String versionId = String.valueOf(recordCount);
            logicalId = patient.getId().split("/");
            patient.setId(new IdType("Obs", logicalId[1], versionId));
            retVal.add(patient);
        }

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setAction(AuditEvent.AuditEventAction.R);
        auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("vread").setDisplay("search"));
        auditEvent.setRecorded(new Date());
        auditEvent.setOutcomeDesc( retVal.size() + " Encounter Resource found");

        rhies_FHIRBALPInterceptor.storeAuditEvent(auditEvent);
        
        return retVal;
    }

    private BasicDBObject buildSearchPattern(String value) {
        return new BasicDBObject("$regex", ".*" + value + ".*").append("$options", "i");
    }

    private List<BasicDBObject> buildNEPattern() {
        BasicDBObject result = new BasicDBObject();
        List<BasicDBObject> objects = new ArrayList<BasicDBObject>();
        objects.add(new BasicDBObject("$ne", "Registration"));
        objects.add(new BasicDBObject("$ne", "Drug Order Encounter"));
        objects.add(new BasicDBObject("$ne", "LAB TEST"));
        return objects;
    }

    /**
     * Implementation of the "Create" (Post) method
     *
     * @throws IOException
     */
    @Create
    public MethodOutcome create(@ResourceParam String IncomingEncounter) throws NullPointerException, IOException {
        MethodOutcome method = new MethodOutcome();
        IParser par = ctx.newJsonParser();
        JsonParser parser = new JsonParser();
        String nida = "";

        //errors handling
        if (IncomingEncounter == null || IncomingEncounter.trim().equals("")) {
            utils.error(Constants.ERROR_PATIENT_EMPTY);
            return method;
        }

        Encounter encounter = par.parseResource(Encounter.class, IncomingEncounter);

        DBCollection patientCollection = CommonDbConnection.dbConnection().getCollection("encounter");
        BasicDBObject query = null;
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setRecorded(new Date());

        // NIDA as first criteria, PCID comes next. But PCID is mandatory. so PCID is main id
        if (nida != null && !nida.trim().equals("")) { //nida exists
            BasicDBObject value = new BasicDBObject("system", "NIDA");
            value.put("value", nida);
            query = new BasicDBObject("identifier", value);
            DBCursor cursor = patientCollection.find(query);

            DBObject dbobject = cursor.one();
            String encoded = par.encodeResourceToString(encounter);
            DBObject doc = (DBObject) JSON.parse(encoded);

            String[] entityValues = getValuesEntities(doc);
            String id = entityValues[0];
            String refer = entityValues[1];
            String refer_type = entityValues[2];
            if((id != null && !id.isEmpty()) || (refer != null && !refer.isEmpty()) || (refer_type != null && !refer_type.isEmpty())){
                AuditEvent.AuditEventEntityComponent event = getAuditEventEntityComponent(id, refer, refer_type);
                auditEvent.addEntity(event);
            }

            String[] values = getValues(doc);
            if(values != null && values.length >= 2) {
                String disp = values[0];
                String ids = values[1];
                String role_type = values[2];
                if ((disp != null && !disp.isEmpty()) || (ids != null && !ids.isEmpty()) || (role_type != null && !role_type.isEmpty())) {
                    AuditEvent.AuditEventAgentComponent event = getAuditEventAgentComponent(ids, disp, role_type);
                    auditEvent.addAgent(event);
                }
            }

            if (dbobject != null) {
                patientCollection.update(query, doc);
                auditEvent.setAction(AuditEvent.AuditEventAction.U);
                auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("update").setDisplay("update"));
                auditEvent.setOutcomeDesc("Encounter request Updated with value: " + value);
            } else {
                //NIDA not exists, check if not exists with PCID
                query = new BasicDBObject("id", encounter.getId().split("/")[1]);
                cursor = patientCollection.find(query);

                dbobject = cursor.one();
                encoded = par.encodeResourceToString(encounter);
                doc = (DBObject) JSON.parse(encoded);

                if (dbobject != null) {
                    patientCollection.update(query, doc);
                    auditEvent.setAction(AuditEvent.AuditEventAction.U);
                    auditEvent.setOutcomeDesc("Encounter Records Updated: id: " + encounter.getId().split("/")[1]);
                    auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("update").setDisplay("update"));
                } else {
                    patientCollection.insert(doc);
                    auditEvent.setAction(AuditEvent.AuditEventAction.C);
                    auditEvent.setOutcomeDesc("Encounter Records Created: id: " + encounter.getId().split("/")[1]);
                    auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("create").setDisplay("create"));
                }
            }
        } else {
            query = new BasicDBObject("id", encounter.getId().split("/")[1]);
            DBCursor cursor = patientCollection.find(query);

            DBObject dbobject = cursor.one();
            String encoded = par.encodeResourceToString(encounter);
            DBObject doc = (DBObject) JSON.parse(encoded);

            String[] entityValues = getValuesEntities(doc);
            String id = entityValues[0];
            String refer = entityValues[1];
            String refer_type = entityValues[2];
            if((id != null && !id.isEmpty()) || (refer != null && !refer.isEmpty()) || (refer_type != null && !refer_type.isEmpty())){
                AuditEvent.AuditEventEntityComponent event = getAuditEventEntityComponent(id, refer, refer_type);
                auditEvent.addEntity(event);
            }

            String[] values = getValues(doc);
            if(values != null && values.length >= 2) {
                String disp = values[0];
                String ids = values[1];
                String role_type = values[2];
                if ((disp != null && !disp.isEmpty()) || (ids != null && !ids.isEmpty()) || (role_type != null && !role_type.isEmpty())) {
                    AuditEvent.AuditEventAgentComponent event = getAuditEventAgentComponent(ids, disp, role_type);
                    auditEvent.addAgent(event);
                }
            }

            if (dbobject != null) {
                patientCollection.update(query, doc);
                auditEvent.setAction(AuditEvent.AuditEventAction.U);
                auditEvent.setOutcomeDesc("Encounter Records Updated: id: " + encounter.getId().split("/")[1]);
                auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("update").setDisplay("update"));
            } else {
                patientCollection.insert(doc);
                auditEvent.setAction(AuditEvent.AuditEventAction.C);
                auditEvent.setOutcomeDesc("Encounter Records Created: id: " + encounter.getId().split("/")[1]);
                auditEvent.setType(new Coding().setSystem("http://hl7.org/fhir/restful-interaction").setCode("create").setDisplay("create"));
            }
        }

        method.setCreated(true);
        rhies_FHIRBALPInterceptor.storeAuditEvent(auditEvent);
        return method;
    }
    private static String[] getValuesEntities(DBObject doc){
        String[] values = new String[3];
        String id = (String) doc.get("id");
        String type = (String) doc.get("resourceType");
        String reference = type+'/'+id;
        values[0] = id;
        values[1] = reference;
        values[2] = type;
        return values;
    }

    private static AuditEvent.AuditEventEntityComponent getAuditEventEntityComponent(String id, String refer, String refer_type) {
        AuditEvent.AuditEventEntityComponent event = new AuditEvent.AuditEventEntityComponent();
        Reference what = new Reference();
        what.setReference(refer);
        Identifier identifier = new Identifier();
        identifier.setValue(id);
        what.setIdentifier(identifier);

        Coding detail_type= new Coding();
        detail_type.setSystem("http://dicom.nema.org/resources/ontology/DCM");
        detail_type.setCode("110110");
        detail_type.setDisplay("Patient Record");


        Coding theCoding= new Coding();
        theCoding.setSystem("http://terminology.hl7.org/CodeSystem/object-role");
        theCoding.setCode("4");
        theCoding.setDisplay("Domain Resource");

        event.setType(detail_type);
        event.setWhat(what);
        event.setRole(theCoding);
        event.setName(refer_type);
        return event;
    }

    private static String[] getValues(DBObject doc){
        String[] values = new String[3];
        BasicDBList participant = (BasicDBList) doc.get("participant");
        String id = "" ;
        String display= "";
        String role_type= "";
        if (participant != null) {
            for (Object scenario : participant) {
                BasicDBObject individual = (BasicDBObject) ((BasicDBObject) scenario).get("individual");
                if (individual != null) {
                    BasicDBObject identifier = (BasicDBObject) individual.get("identifier");
                    if (identifier != null) {
                        id = (String) identifier.get("value");
                    }
                    display = (String) individual.get("display");
                    role_type = (String) individual.get("type");
                    values[0] = display;
                    values[1] = id;
                    values[2] = role_type;
                }
            }
            return values;
        }
        return null;
    }
    private static AuditEvent.AuditEventAgentComponent getAuditEventAgentComponent(String id, String display, String role_type) {
        AuditEvent.AuditEventAgentComponent event = new AuditEvent.AuditEventAgentComponent();
        Identifier identifier = new Identifier();
        identifier.setValue(id);
        Reference who = new Reference();
        who.setDisplay(display);
        who.setIdentifier(identifier);

        Coding theCoding= new Coding();
        theCoding.setSystem("http://terminology.hl7.org/CodeSystem/extra-security-role-type");
        theCoding.setCode("humanuser");
        theCoding.setDisplay("human user");
        CodeableConcept codeableConcept = new CodeableConcept();
        List <Coding> codeList = new ArrayList<Coding>();
        codeList.add(theCoding);
        codeableConcept.setCoding(codeList);

        CodeableConcept codeableConcept2 = new CodeableConcept();
        codeableConcept2.setText(role_type);
        List <CodeableConcept> roleList = new ArrayList<CodeableConcept>();
        roleList.add(codeableConcept2);

        event.setRole(roleList);
        event.setType(codeableConcept);
        event.setWho(who);
        event.setRequestor(true);
        return event;
    }
}
