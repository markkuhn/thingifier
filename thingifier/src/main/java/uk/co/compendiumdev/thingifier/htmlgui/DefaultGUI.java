package uk.co.compendiumdev.thingifier.htmlgui;

import com.google.gson.GsonBuilder;
import uk.co.compendiumdev.thingifier.Thing;
import uk.co.compendiumdev.thingifier.Thingifier;
import uk.co.compendiumdev.thingifier.ThingifierApiConfig;
import uk.co.compendiumdev.thingifier.generic.FieldType;
import uk.co.compendiumdev.thingifier.generic.definitions.Field;
import uk.co.compendiumdev.thingifier.generic.definitions.FieldValue;
import uk.co.compendiumdev.thingifier.generic.definitions.RelationshipVector;
import uk.co.compendiumdev.thingifier.generic.definitions.ThingDefinition;
import uk.co.compendiumdev.thingifier.generic.instances.ThingInstance;
import uk.co.compendiumdev.thingifier.reporting.JsonThing;
import uk.co.compendiumdev.thingifier.reporting.XmlThing;

import java.util.Collection;

import static spark.Spark.get;

public class DefaultGUI {

    // TODO: templates or tidier way to create the default GUI pages with styling

    private final Thingifier thingifier;
    private final JsonThing jsonThing;
    private final DefaultGUIHTMLRootMenu rootMenu;
    private final XmlThing xmlThing;
    private final ThingifierApiConfig apiConfig;

    public DefaultGUI(final Thingifier thingifier) {
        this.thingifier=thingifier;
        this.apiConfig = thingifier.apiConfig();
        this.jsonThing = new JsonThing(apiConfig.jsonOutput());
        this.xmlThing = new XmlThing(jsonThing);
        this.rootMenu = new DefaultGUIHTMLRootMenu();
    }

    public void setupDefaultGUI(){

        get("/gui", (request, response) -> {
            response.type("text/html");
            response.status(200);
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>GUI</title></head></body>");
            html.append(rootMenu.getMenuAsHTML());
            html.append("</body></html>");
            return html.toString();
        });

        get("/gui/entities", (request, response) -> {
            response.type("text/html");
            response.status(200);
            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>GUI</title></head></body>");
            html.append(getInstancesRootMenuHtml());
            html.append("</body></html>");
            return html.toString();
        });

        get("/gui/instances", (request, response) -> {
            response.type("text/html");
            response.status(200);
            StringBuilder html = new StringBuilder();

            String entityName = request.queryParams("entity");

            html.append("<html><head><title>GUI</title></head><body>");
            html.append(getInstancesRootMenuHtml());
            html.append(getHTMLTableStylingCSS());


            final Thing thing = thingifier.getThingNamed(entityName);
            final ThingDefinition definition = thing.definition();

            html.append("<h2>" + definition.getPlural() + "</h2>");

            html.append(startHtmlTableFor(definition));

            for(ThingInstance instance : thing.getInstances()){
                html.append(htmlTableRowFor(instance));
            }
            html.append("</table>");
            html.append("</body></html>");
            return html.toString();
        });

        get("/gui/instance", (request, response) -> {
            response.type("text/html");
            response.status(200);
            StringBuilder html = new StringBuilder();

            String entityName;
            Thing thing=null;
            for(String queryParam : request.queryParams()){
                if(queryParam.contentEquals("entity")){
                    entityName = request.queryParams("entity");
                    thing = thingifier.getThingNamed(entityName);
                }
            }
            String keyName="";
            String keyValue="";
            for(String queryParam : request.queryParams()){
                Field field = thing.definition().getField(queryParam);
                if(field!=null) {
                    if (field.getType() == FieldType.GUID || field.getType() == FieldType.ID) {
                        keyName = field.getName();
                        keyValue = request.queryParams(queryParam);
                        break;
                    }
                }
            }

            final ThingDefinition definition = thing.definition();
            ThingInstance instance = thing.findInstanceByField(FieldValue.is(keyName, keyValue));

            html.append("<html><head><title>GUI</title></head></body>");
            html.append(getInstancesRootMenuHtml());

            html.append("<h2>" + definition.getName() + "</h2>");

            html.append("<ul>");
            if(apiConfig.showGuidsInResponses()) {
                html.append(String.format("<li>%s<ul><li>%s</li></ul></li>", "guid", instance.getGUID()));
            }
            for(String field : definition.getFieldNames()) {
                if (!field.equals("guid")) {
                    html.append(String.format("<li>%s<ul><li>%s</li></ul></li>",
                            field, instance.getValue(field)));
                }
            }
            html.append("</ul>");

            if(instance.hasAnyRelationshipInstances()) {

                html.append("<h2>Relationships</h2>");
                html.append(getHTMLTableStylingCSS());

                for (RelationshipVector relationship : definition.getRelationships()) {
                    final Collection<ThingInstance> relatedItems = instance.connectedItems(relationship.getName());
                    html.append("<h2>" + relationship.getName() + "</h2>");
                    if(relatedItems.size() > 0) {
                        boolean header=true;

                        for (ThingInstance relatedInstance : relatedItems) {
                            if(header){
                                html.append(startHtmlTableFor(relatedInstance.getEntity()));
                                header=false;
                            }
                            html.append(htmlTableRowFor(relatedInstance));
                        }
                        html.append("</table>");

                    }else{
                        html.append("<ul><li>none</li></ul>");
                    }
                }
            }

            html.append("<h2>JSON Example</h2>");
            html.append("<pre>");
            html.append("<code class='json'>");
            // pretty print the json
            html.append(new GsonBuilder().setPrettyPrinting()
                    .create().toJson(jsonThing.asJsonObject(instance)));
            html.append("</code>");
            html.append("</pre>");


            html.append("<h2>XML Example</h2>");
            html.append("<pre>");
            html.append("<code class='xml'>");
            // pretty print the json
            html.append(xmlThing.prettyPrintHtml(xmlThing.getSingleObjectXml(instance)));
            html.append("</code>");
            html.append("</pre>");
            html.append("</body></html>");
            return html.toString();
        });
    }

    private String getInstancesRootMenuHtml() {
        StringBuilder html = new StringBuilder();
        html.append(rootMenu.getMenuAsHTML());
        html.append("<div class='entity-instances-menu'>");
        html.append("<ul>");
        for(String thing : thingifier.getThingNames()){
            html.append(String.format("<li><a href='/gui/instances?entity=%1$s'>%1$s</a></li>",thing));
        }
        html.append("</ul>");
        html.append("</div>");
        return html.toString();
    }

    private String getHTMLTableStylingCSS() {
        StringBuilder html = new StringBuilder();
        html.append("<style>\n");
        html.append("table {border-collapse: collapse;}\n" +
                "table, th, td {border: 1px solid black;}\n");
        html.append("</style>");
        return html.toString();
    }

    private String htmlTableRowFor(final ThingInstance instance) {
        StringBuilder html = new StringBuilder();
        final ThingDefinition definition = instance.getEntity();

        html.append("<tr>");
        // show keys first
        if(apiConfig.showGuidsInResponses()) {
            html.append(String.format("<td><a href='/gui/instance?entity=%1$s&guid=%2$s'>%2$s</a></td>",
                    definition.getName(), instance.getGUID()));
        }

        // show any clickable id fields
        for(String field : definition.getFieldNames()) {
            if (!field.equals("guid")) {
                Field theField = definition.getField(field);
                if(theField.getType()== FieldType.ID){
                    // make ids clickable
                    String renderAs = String.format("<a href='/gui/instance?entity=%1$s&%2$s=%3$s'>%3$s</a>",
                            definition.getName(),theField.getName(), instance.getValue(field));
                    html.append(String.format("<td>%s</td>", renderAs));
                }

            }
        }

        // show any normal fields
        for(String field : definition.getFieldNames()) {
            Field theField = definition.getField(field);
            if(theField.getType()!= FieldType.ID && theField.getType()!=FieldType.GUID){
                html.append(String.format("<td>%s</td>", instance.getValue(field)));
            }
        }
        html.append("</tr>");

        return html.toString();
    }

    private String startHtmlTableFor(final ThingDefinition definition) {
        StringBuilder html = new StringBuilder();

        html.append("<table>");
        html.append("<tr>");
        // guid first
        if(apiConfig.showGuidsInResponses()) {
            html.append("<th>guid</th>");
        }
        // then any ids
        for(String field : definition.getFieldNames()) {
            Field theField = definition.getField(field);
            if (theField.getType()==FieldType.ID) {
                html.append(String.format("<th>%s</th>", field));
            }
        }
        // then the normal fields
        for(String field : definition.getFieldNames()) {
            Field theField = definition.getField(field);
            if (theField.getType()!=FieldType.ID && theField.getType()!=FieldType.GUID) {
                html.append(String.format("<th>%s</th>", field));
            }
        }
        html.append("</tr>");

        return html.toString();
    }
}

