package org.intermine.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.intermine.api.InterMineAPITestCase;
import org.intermine.api.bag.Group;
import org.intermine.api.bag.SharedBagManager;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.BagValue;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.SavedQuery;
import org.intermine.api.profile.TagManager;
import org.intermine.api.template.ApiTemplate;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.ProfileBinding;
import org.intermine.web.ProfileManagerBinding;

import com.sun.org.apache.xml.internal.serializer.SerializerTrace;

public class ProfileManagerBindingTest extends InterMineAPITestCase
{

    public ProfileManagerBindingTest(String arg) {
        super(arg);
    }

    private Profile bobProfile, sallyProfile;
    private ProfileManager pm;
    private final Integer bobId = new Integer(101);
    private final Integer sallyId = new Integer(102);
    private final String bobPass = "bob_pass";
    private final String sallyPass = "sally_pass";
    private Map<String, List<FieldDescriptor>>  classKeys;

    private final String bobKey = "BOBKEY";

    public void setUp() throws Exception {
        super.setUp();
        StoreDataTestCase.oneTimeSetUp();
        classKeys = im.getClassKeys();
        pm = im.getProfileManager();
        setUpUserProfiles();
        configureXUnit();
    }
    
    private void configureXUnit() {
        XMLUnit.setControlParser("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        XMLUnit.setTestParser("org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
        XMLUnit.setSAXParserFactory("org.apache.xerces.jaxp.SAXParserFactoryImpl");
        XMLUnit.setTransformerFactory("org.apache.xalan.processor.TransformerFactoryImpl");
    }

    public void tearDown() throws Exception {
        try {
            super.tearDown();
        } catch (Throwable t) {
            System.err.println("[WARNING] error during teardown: " + t);
        }
        try {
            StoreDataTestCase.oneTimeTearDown();
        } catch (Throwable t) {
            System.err.println("[WARNING] error during teardown: " + t);
        }
    }

    private void setUpUserProfiles() throws Exception {
        PathQuery query = new PathQuery(Model.getInstanceByName("testmodel"));
        Date date = new Date();
        SavedQuery sq = new SavedQuery("query1", date, query);

        // bob's details
        String bobName = "bob";
        List<String> classKeys = new ArrayList<String>();
        classKeys.add("name");
        InterMineBag bag = new InterMineBag("bag1", "Department", "This is some description",
                new Date(), BagState.CURRENT, os, bobId, uosw, classKeys);

        Department deptEx = new Department();
        deptEx.setName("DepartmentA1");
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("name");
        Department departmentA1 = (Department) os.getObjectByExample(deptEx, fieldNames);
        bag.addIdToBag(departmentA1.getId(), "Department");

        Department deptEx2 = new Department();
        deptEx2.setName("DepartmentB1");
        Department departmentB1 = (Department) os.getObjectByExample(deptEx2, fieldNames);
        bag.addIdToBag(departmentB1.getId(), "Department");

        ApiTemplate template =
            new ApiTemplate("template", "ttitle", "tcomment",
                              new PathQuery(Model.getInstanceByName("testmodel")));

        bobProfile = new Profile(pm, bobName, bobId, bobPass,
                Collections.EMPTY_MAP, Collections.EMPTY_MAP, Collections.EMPTY_MAP, bobKey,
                true, false);
        pm.createProfile(bobProfile);
        bobProfile.saveQuery("query1", sq);
        bobProfile.saveBag("bag1", bag);
        bobProfile.saveTemplate("template", template);

        query = new PathQuery(Model.getInstanceByName("testmodel"));
        sq = new SavedQuery("query1", date, query);

        // sally details
        String sallyName = "sally";

        // employees and managers
        //    <bag name="sally_bag2" type="org.intermine.model.CEO">
        //        <bagElement type="org.intermine.model.CEO" id="1011"/>
        //    </bag>

        CEO ceoEx = new CEO();
        ceoEx.setName("EmployeeB1");
        fieldNames = new HashSet<String>();
        fieldNames.add("name");
        CEO ceoB1 = (CEO) os.getObjectByExample(ceoEx, fieldNames);

        InterMineBag objectBag = new InterMineBag("bag2", "Employee", "description",
                new Date(), BagState.CURRENT, os, sallyId, uosw, classKeys);
        objectBag.addIdToBag(ceoB1.getId(), "CEO");

        template = new ApiTemplate("template", "ttitle", "tcomment",
                                     new PathQuery(Model.getInstanceByName("testmodel")));
        sallyProfile = new Profile(pm, sallyName, sallyId, sallyPass,
                  Collections.EMPTY_MAP, Collections.EMPTY_MAP, Collections.EMPTY_MAP, true, false);
        pm.createProfile(sallyProfile);
        sallyProfile.saveQuery("query1", sq);
        sallyProfile.saveBag("sally_bag1", objectBag);

        sallyProfile.saveTemplate("template", template);

        TagManager tagManager = im.getTagManager();

        tagManager.addTag("test-tag", "Department.company", "reference", bobProfile);
        tagManager.addTag("test-tag2", "Department.name", "attribute", bobProfile);
        tagManager.addTag("test-tag2", "Department.company", "reference", bobProfile);
        tagManager.addTag("test-tag2", "Department.employees", "collection", bobProfile);

        tagManager.addTag("test-tag", "Department.company", "reference", sallyProfile);

        SharedBagManager sbm = SharedBagManager.getInstance(pm);
        Group bobsGroup = sbm.createGroup(bobProfile, "bobs-group", "A group for bob and his friends");
        sbm.addUserToGroup(bobsGroup, sallyProfile);
        sbm.shareBagWithGroup(bobProfile, "bag1", bobsGroup);
    }
    
    public void testMarshal() throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
            writer.writeStartElement("userprofiles");
            ProfileBinding.marshal(bobProfile, pm, writer);
            ProfileBinding.marshal(sallyProfile, pm, writer);
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

        InputStream is = getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTest.xml");

        String control = IOUtils.toString(is), test = sw.toString();
        Diff diff = new Diff(control, test);
        assertTrue("XML is identical " + diff.toString(), diff.identical());
    }

    public void testUnmarshal() throws Exception {
        fail("Skip me for a bit...");
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("ProfileManagerBindingTestNewIDs.xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        ProfileManagerBinding.unmarshal(reader, pm, os.getNewWriter());

        assertEquals(4, pm.getProfileUserNames().size());

        assertTrue(pm.getProfileUserNames().contains("Unmarshall-1"));

        Profile stored2 = pm.getProfile("Unmarshall-2", "querty");

        assertEquals("token2", stored2.getApiKey());

        Employee employeeEx = new Employee();
        employeeEx.setName("EmployeeA3");
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("name");

        assertEquals("Wrong number of bags!", 3, stored2.getSavedBags().size());
        Set<Integer> expectedBagContents = new HashSet<Integer>();
        //when we read xml file, we load data into savedbag and bagvalues table but not in the
        //osbag_int loaded after user login
        assertEquals(expectedBagContents,
                    (stored2.getSavedBags().get("stored_2_3")).getContentsAsIds());

        List<BagValue> contentsAsKey = (stored2.getSavedBags()
                .get("stored_2_1")).getContents();
        assertEquals("DepartmentA1", contentsAsKey.get(0).getValue());

        List<BagValue> contentsAsKey2 = (stored2.getSavedBags()
                .get("stored_2_3")).getContents();
        assertEquals("EmployeeA3", contentsAsKey2.get(0).getValue());
        assertEquals("EmployeeB2", contentsAsKey2.get(1).getValue());

        assertEquals(1, stored2.getSavedQueries().size());
        assertEquals(1, stored2.getSavedTemplates().size());

        Set<Tag> expectedTags = new HashSet<Tag>();
        Tag tag1 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));

        tag1.setTagName("test-tag");
        tag1.setObjectIdentifier("Department.company");
        tag1.setType("reference");
        tag1.setUserProfile(pm.getUserProfile("Unmarsall-1"));

        Tag tag2 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));
        tag2.setTagName("test-tag2");
        tag2.setObjectIdentifier("Department.name");
        tag2.setType("attribute");
        tag2.setUserProfile(pm.getUserProfile("Unmarsall-1"));

        Tag tag3 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));
        tag3.setTagName("test-tag2");
        tag3.setObjectIdentifier("Department.company");
        tag3.setType("reference");
        tag3.setUserProfile(pm.getUserProfile("Unmarsall-1"));

        Tag tag4 = (Tag) DynamicUtil.createObject(Collections.singleton(Tag.class));
        tag4.setTagName("test-tag2");
        tag4.setObjectIdentifier("Department.employees");
        tag4.setType("collection");
        tag4.setUserProfile(pm.getUserProfile("Unmarsall-1"));

        expectedTags.add(tag1);
        expectedTags.add(tag2);
        expectedTags.add(tag3);
        expectedTags.add(tag4);

        Set<Tag> actualTags = new HashSet<Tag>(im.getTagManager()
                .getTags(null, null, null, "Unmarshall-1"));

        assertEquals(expectedTags.size(), actualTags.size());

        Iterator<Tag> actualTagsIter = actualTags.iterator();

      ACTUAL:
        while (actualTagsIter.hasNext()) {
            Tag actualTag = actualTagsIter.next();

            Iterator<Tag> expectedTagIter = expectedTags.iterator();

            while (expectedTagIter.hasNext()) {
                Tag expectedTag = expectedTagIter.next();
                if (actualTag.getTagName().equals(expectedTag.getTagName())
                    && actualTag.getObjectIdentifier().equals(expectedTag.getObjectIdentifier())
                    && actualTag.getType().equals(expectedTag.getType())
                    && "Unmarshall-1".equals(actualTag.getUserProfile().getUsername())) {
                    continue ACTUAL;
                }
            }

            fail("can't find tag " + actualTag.getTagName() + ", "
                 + actualTag.getObjectIdentifier() + ", "
                 + actualTag.getType());
        }
    }

}