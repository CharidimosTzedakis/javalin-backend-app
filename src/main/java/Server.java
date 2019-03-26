import io.javalin.Context;
import io.javalin.Javalin;
import io.javalin.security.Role;
import io.javalin.validation.JavalinValidation;

import java.time.Instant;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.security.SecurityUtil.roles;

public class Server {

    enum MyRole implements Role {
        ANYONE, ADMIN
    }

    /**
     * getUserRole(): get user role by checking request headers
     * @param ctx
     * @return
     */
    static MyRole getUserRole(Context ctx) {
        // determine user role based on request
        // typically done by inspecting headers
        String AuthorizationHeader = ctx.req.getHeader("Authorization");
        if (AuthorizationHeader != null) return MyRole.ADMIN;
        return MyRole.ANYONE;
    }

    public static void main(String[] args) {
        Javalin app = Javalin.create();
        JavalinValidation.register(Instant.class, v -> Instant.parse(v));

        //JavalinValidation.register(Instant::java.class, v -> Instant.ofEpochMilli(v.toLong()));


        /**
         * Access Management setup
         */
        app.accessManager((handler, ctx, permittedRoles) -> {
            MyRole userRole = getUserRole(ctx);
            if (permittedRoles.contains(userRole)) {
                handler.handle(ctx);
            } else {
                ctx.status(401).result("Unauthorized");
            }
        });

        app.start(7000);

        app.routes(() -> {
            get("/un-secured",   ctx -> ctx.result("Hello"),   roles(MyRole.ANYONE));
            get("/secured",      ctx -> ctx.result("Hello"),   roles(MyRole.ADMIN));
        });


        app.get("/", ctx -> ctx.result("Hello World! Service is running!"));

        /**
         * handling various input
         */
        app.get("/test/:path-param", ctx -> {
            String qp = ctx.queryParam("query-param");
            String pp = ctx.pathParam("path-param");
            String fp = ctx.formParam("form-param");
            String body = ctx.body();
            //! will use Jackson library to deserialize the JSON
            Person myPerson = ctx.bodyAsClass(Person.class);
            System.out.println(myPerson.toString());

            //validating params
            int index = ctx.validatedQueryParam("index").asInt().getOrThrow();
        });

        /**
         * validating time params
         */
        app.get("/time-interval", ctx -> {
            Instant fromDate = ctx.validatedQueryParam("from")
                    .asClass(Instant.class)
                    .getOrThrow();

            Instant toDate = ctx.validatedQueryParam("to")
                    .asClass(Instant.class)
                    .check(to -> to.isAfter(fromDate), "'to' has to be after 'from'")
                    .getOrThrow();
        });

        /**
         * handle general exceptions
         */
        app.exception(Exception.class, (e, ctx) -> {
            // handle general exceptions here
            // will not trigger if more specific exception-mapper found
            ctx.status(404);
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace().toString());

        });
    }
}
