package iTests.framework.testng.report.xml;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

public class CustomizedDomDriver extends DomDriver{
    /**The list that contains the field names that would contain a CDATA in the xml.*/
    private static final List CDATA_FIELDS;

    static{
        CDATA_FIELDS = new ArrayList();
        //add cdata field names.eg:cdataField
        CDATA_FIELDS.add("cause");
    }

    @Override
    public HierarchicalStreamWriter createWriter(Writer out){
        return new PrettyPrintWriter(out){
            boolean cdata = false;

            public void startNode(String name){
                super.startNode(name);
                cdata = CDATA_FIELDS.contains(name);

            }

            protected void writeText(QuickWriter writer, String text){
                if (cdata){
                    writer.write("<![CDATA[");
                    writer.write(text);
                    writer.write("]]>");
                }
                else{
                    writer.write(text);
                }
            }
        };

    }

}
