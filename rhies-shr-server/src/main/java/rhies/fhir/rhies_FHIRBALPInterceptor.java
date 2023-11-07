package rhies.fhir;

import org.hl7.fhir.r4.model.AuditEvent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;


import java.net.UnknownHostException;


public class rhies_FHIRBALPInterceptor extends InterceptorAdapter {
    static FhirContext fhirContext = FhirContext.forR4();
    static IParser iParser = fhirContext.newJsonParser();


    public static void storeAuditEvent(AuditEvent auditEvent) throws UnknownHostException {
        // Implement the logic to store the audit event in your audit log storage
        // This can be a database, a log file, an external service, etc.

       System.out.println("\n=========================================\n" +
          auditEvent.getOutcomeDesc() + "\n" +
          auditEvent.getAction().name() + "\n" +
          auditEvent.getOutcomeDesc() + "\n" +
          "\n=========================================\n");

       DBCollection auditEventCollection = CommonDbConnection.dbConnection().getCollection("audit_events");

       BasicDBObject query = null;
       String auditEventEncoded = iParser.encodeResourceToString(auditEvent);

       DBObject dbObject = (DBObject) JSON.parse(auditEventEncoded);

       auditEventCollection.insert(dbObject);
    }
    
}
