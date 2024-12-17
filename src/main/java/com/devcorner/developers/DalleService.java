package com.devcorner.developers;


import dev.langchain4j.model.image.ImageModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

@ApplicationScoped
@Path( "/dalle")
public class DalleService {

    private static final Logger LOG = Logger.getLogger(DalleService.class.getName());

    @Inject
    ImageModel imageModel;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateImage(@QueryParam("name") String name,
                                  @QueryParam("description") String description) throws IOException {

        var image = imageModel.generate(description);
        var uri = image.content().url();
        var desc = image.content().revisedPrompt();

        String fname = name.replaceAll("\\s", "");
        fname = fname.toLowerCase();
        var file = new File(fname + ".jpg");
        Files.copy(uri.toURL().openStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        LOG.info("file://" + file.getAbsolutePath());

        var descFile = new File(fname + ".txt");
        Files.writeString(descFile.toPath(), desc);
        LOG.info(desc);

        return Response
                .seeOther(uri)
                .build();
    }

    @GET
    @Path("/getExistingImage")
    @Produces("image/jpeg")
    public Response getExistingImage(@QueryParam("name") String name) throws IOException {

        LOG.info("getPreviouslyGeneratedImage");

        String fname = name.replaceAll("\\s", "");
        fname = fname.toLowerCase();
        LOG.info("file://" + fname + ".jpg");
        File imageFile = new File("./" + fname + ".jpg");

        if (!imageFile.exists()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader descFile = new BufferedReader(new FileReader("./" + fname + ".txt"));

            String line;
            while ((line = descFile.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException ex) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(imageFile)
                .header("Content-Disposition", "attachment; filename=\"" + imageFile.getName() + "\"")
                .header("X-Image-Description", sb.toString())
                .build();
    }

}
