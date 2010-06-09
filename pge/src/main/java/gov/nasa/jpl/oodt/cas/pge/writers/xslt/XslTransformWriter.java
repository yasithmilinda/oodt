/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package gov.nasa.jpl.oodt.cas.pge.writers.xslt;

//JDK imports
import java.io.File;
import java.io.IOException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;

//OODT imports
import gov.nasa.jpl.oodt.cas.metadata.Metadata;
import gov.nasa.jpl.oodt.cas.metadata.SerializableMetadata;
import gov.nasa.jpl.oodt.cas.pge.writers.SciPgeConfigFileWriter;

/**
 * 
 * @author bfoster
 * @version $Revision$
 * 
 * <p>
 * XSL Transformation class which writes Science PGE config files based from the
 * XML format of SerializableMetadata
 * </p>.
 */
public class XslTransformWriter implements SciPgeConfigFileWriter {

    public File createConfigFile(String sciPgeConfigFilePath,
            Metadata inputMetadata, Object... customArgs) throws IOException {
        try {
            File sciPgeConfigFile = new File(sciPgeConfigFilePath);

            String xsltFilePath = (String) customArgs[0];
            Source xsltSource = new StreamSource(new File(xsltFilePath));
            Result result = new StreamResult(sciPgeConfigFile);

            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);
            boolean useCDATA = customArgs.length > 1 ? ((String) customArgs[1])
                    .toLowerCase().equals("true") : false;
            Source xmlSource = new DOMSource((new SerializableMetadata(
                    inputMetadata,
                    trans.getOutputProperty(OutputKeys.ENCODING), useCDATA))
                    .toXML());

            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.transform(xmlSource, result);

            return sciPgeConfigFile;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to create science PGE config file '"
                    + sciPgeConfigFilePath + "' : " + e.getMessage());
        }
    }

}
