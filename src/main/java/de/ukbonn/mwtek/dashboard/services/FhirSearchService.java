/*
 * Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 * modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 * PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 * OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 * YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 * OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 * COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 * BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 * OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 * PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 * this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */

package de.ukbonn.mwtek.dashboard.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.ukbonn.mwtek.dashboard.auth.RestConsumer;
import de.ukbonn.mwtek.dashboard.configuration.FhirSearchConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.FhirServerRestConfiguration;
import de.ukbonn.mwtek.dashboard.interfaces.SearchService;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for providing the {@link FhirSearchConfiguration} (e.g. which Loinc codes are to be used)
 * from <code>application.yaml</code>.
 *
 * @author <a href="mailto:david.meyers@ukbonn.de">David Meyers</a>
 */
@Slf4j
@Service
public class FhirSearchService extends RestConsumer implements SearchService {

  FhirContext ctx = FhirContext.forR4();

  protected FhirServerRestConfiguration fhirServerConf;

  @Autowired
  public FhirSearchService(FhirServerRestConfiguration fhirServerConf) {
    super(fhirServerConf);
    this.fhirServerConf = fhirServerConf;
  }

  /**
   * Retrieve a FHIR search query and parse the result in FHIR {@link Bundle#getEntry() bundle entry
   * components} This method is used when pagination is not necessary.
   *
   * @param querySuffix The suffix with the FHIR search logic to be appended to the FHIR server
   *     endpoint url (e.g. Patient?id=1).
   * @return The response from the FHIR search query, parsed into a FHIR {@link Bundle#getEntry()
   *     bundle entry components}
   */
  public List<BundleEntryComponent> getBundleData(String querySuffix) {
    IParser parser = ctx.newJsonParser();
    String fhirServerEndpoint = this.fhirServerConf.getRestUrl();

    RestTemplate rest = this.getRestTemplate();
    String restUrl = fhirServerEndpoint + querySuffix;
    log.debug(restUrl);
    ResponseEntity<String> searchRequest = rest.getForEntity(restUrl, String.class);
    Bundle requestBundle = parser.parseResource(Bundle.class, searchRequest.getBody());
    return requestBundle.getEntry();
  }

  /**
   * If the query of all entries of a particular FHIR resource needs to be split (for performance
   * reasons, for example), the meta-information must be retrieved at bundle level (e.g. the link to
   * the next offset), not at bundle entry level.
   *
   * @param querySuffix The suffix with the FHIR search logic to be appended to the FHIR server
   *     endpoint url (e.g. Patient?id=1).
   * @return The response from the FHIR search query, parsed into a FHIR {@link Bundle} object
   */
  public Bundle getInitialBundle(String querySuffix) {
    IParser parser = ctx.newJsonParser();
    String fhirServerEndpoint = this.fhirServerConf.getRestUrl();
    RestTemplate rest = this.getRestTemplate();
    String restUrl = fhirServerEndpoint + querySuffix;
    log.debug(restUrl);
    ResponseEntity<String> searchRequest = rest.getForEntity(restUrl, String.class);
    return parser.parseResource(Bundle.class, searchRequest.getBody());
  }

  /**
   * After execution, the initial FHIR search query returns the attributes "self" and "next" within
   * the attribute "link" if the number of resources to be retrieved is too large. These values can
   * be used to control pagination and are used to retrieve additional {@link
   * BundleLinkComponent#getUrl() FhirBundleParts} Since the URL is already supplied in the "next"
   * attribute (in contrast to method {@link #getInitialBundle(String)}) simplified handling is
   * possible here, since the FHIR search query no longer has to be assembled.
   *
   * @param linkToNextPart The FHIR search query to retrieve the next FHIR {@link Bundle} block via
   *     {@link Bundle#getLink()}.
   * @return The response from the FHIR search query, parsed into a FHIR {@link Bundle} object.
   */
  public Bundle getBundlePart(String linkToNextPart) {
    IParser parser = ctx.newJsonParser();
    RestTemplate rest = this.getRestTemplate();
    // The resource.link.next URL can be URL encoded (e.g. on the Blaze server). This allows, for
    // example, comma-separated code arrays to be interpreted as a single string. That's why we
    // decode the URL again. (Alternatively, you could also adjust the header).
    String linkToNextPartDecoded = URLDecoder.decode(linkToNextPart, Charset.defaultCharset());
    log.debug(linkToNextPartDecoded);

    ResponseEntity<String> searchRequest = rest.getForEntity(linkToNextPartDecoded, String.class);
    return parser.parseResource(Bundle.class, searchRequest.getBody());
  }
}
