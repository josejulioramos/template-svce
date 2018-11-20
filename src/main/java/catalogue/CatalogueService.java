package catalogue;

import io.javalin.Context;
import io.javalin.Javalin;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class CatalogueService {


    private static final Logger logger = LoggerFactory.getLogger(CatalogueService.class);

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7000);

        String db_user = System.getenv("DB_USERNAME");
        String db_password = System.getenv("DB_PASSWORD");


        if (db_user != null & db_password != null)
            // Travis magically hides all secure env variables longer than 3 chars - seemingly using a simple case sensitive string match
            // and replaces them with [secure] in the console.  Changing them to upper case to subvert this feature.
            logger.debug("Database credentials: username = {}, password = {}", db_user.toUpperCase(), db_password.toUpperCase());
        else
            logger.error("Database credentials missing: username = {}, password = {}", db_user, db_password);

        app.get("/ping", ctx -> ctx.result("Hello World "));

        app.get("/search", ctx -> {
          Output out = new Output();
          out.result = "search results";
          ctx.json(out);
        } );

        // should be a POST
        app.get("/process", CatalogueService::spawnBatchJob);
    }

    /**
     * apiVersion: batch/v1
     * kind: Job
     * metadata:
     *   name: pi
     * spec:
     *   template:
     *     spec:
     *       containers:
     *       - name: pi
     *         image: perl
     *         command: ["perl",  "-Mbignum=bpi", "-wle", "print bpi(2000)"]
     *       restartPolicy: Never
     *   backoffLimit: 4
     * @param ctx
     */


    public static void spawnBatchJob(Context ctx) {


        try {

            V1Job job = launchBatch(null, "eo-user-compute");
            ctx.json(job);

        } catch (IOException e) {
            ctx.status(500);
        }
    }


    /**
     * To launch a batch job need:
     *
     * - The Job definition
     * - The target namespace where the job (containers) will execute
     * - The K8S API credentials to launch the job - in this case a service account
     */

    public static V1Job launchBatch(V1Job job, String namespace) throws IOException {

        // attempts to work out where the code is running.  If inside a cluster it will locate the
        // container's/pod's service account CA cert and service account token from their mount paths
        // and create a token from them
        //Config.fromCluster()  assumes running inside a cluster - use defaultClient instead
        ApiClient apiClient = Config.defaultClient();


        //Configuration.setDefaultApiClient(apiClient)
        BatchV1Api apiInstance = new BatchV1Api(apiClient);

        String pretty = "true"; // String | If 'true', then the output is pretty printed.
        try {
            V1Job result = apiInstance.createNamespacedJob(namespace, job, pretty);
            return result;
        } catch (ApiException e) {  // TODO improve this
            System.err.println("Exception when calling BatchV1Api#createNamespacedJob");
            throw new RuntimeException(e);
        }
    }


    public static V1Job defineJob() {
        V1Job job = new V1Job();

        job.apiVersion("batch/v1");
        job.kind("Job");


        return job;
    }
}


class Output {

    String result;

    public String getResult() {
        return result;
    }

    public void setResult(String res) {
        result = res;
    }   
}
