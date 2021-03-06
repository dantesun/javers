package org.javers.core

import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import groovy.json.JsonSlurper
import org.javers.core.diff.DiffAssert
import org.javers.core.diff.changetype.NewObject
import org.javers.core.diff.custom.CustomBigDecimalComparator
import org.javers.core.json.DummyPointJsonTypeAdapter
import org.javers.core.json.DummyPointNativeTypeAdapter
import org.javers.core.metamodel.object.UnboundedValueObjectId
import org.javers.core.model.*
import spock.lang.Specification

import static org.javers.core.JaversBuilder.javers
import static org.javers.core.metamodel.object.InstanceIdDTO.instanceId
import static org.javers.core.model.DummyUser.Sex.FEMALE
import static org.javers.core.model.DummyUser.Sex.MALE
import static org.javers.core.model.DummyUserWithPoint.userWithPoint
import static org.javers.test.builder.DummyUserBuilder.dummyUser

/**
 * @author bartosz walacik
 */
class JaversDiffE2ETest extends Specification {

    def "should support custom comparator for Value types"(){
        given:
        def left = new DummyUserWithValues("user", 10.11)
        def right = new DummyUserWithValues("user", 10.12)

        when:
        def javers = JaversBuilder.javers().build()

        then:
        javers.compare(left,right).hasChanges()

        when:
        javers = JaversBuilder.javers()
                .registerCustomComparator(new CustomBigDecimalComparator(1), BigDecimal).build()

        then:
        !javers.compare(left,right).hasChanges()
    }

    def "should support custom comparator for custom types"() {
        given:
        def left =  new GuavaObject(multimap: Multimaps.forMap(["a":1]))
        def right = new GuavaObject(multimap: Multimaps.forMap(["a":2]))
        def javers = JaversBuilder.javers()
                .registerCustomComparator(new CustomMultimapFakeComparator(), Multimap).build()

        when:
        def diff = javers.compare(left,right)

        then:
        diff.changes.size() == 1
        with(diff.changes[0]) {
            affectedCdoId instanceof UnboundedValueObjectId
            property.name == "multimap"
            changes[0].key == "a"
            changes[0].leftValue == 1
            changes[0].rightValue == 2
        }
    }

    def "should create NewObject for all nodes in initial diff"() {
        given:
        def javers = JaversTestBuilder.newInstance()
        DummyUser left = dummyUser("kazik").withDetails().build()

        when:
        def diff = javers.initial(left)

        then:
        DiffAssert.assertThat(diff).has(2, NewObject)
    }

    def "should not create properties snapshot of NewObject by default"() {
        given:
        def javers = JaversBuilder.javers().build()
        def left =  new DummyUser(name: "kazik")
        def right = new DummyUser(name: "kazik", dummyUserDetails: new DummyUserDetails(id: 1, someValue: "some"))

        when:
        def diff = javers.compare(left, right)

        then:
        DiffAssert.assertThat(diff).hasChanges(2)
                  .hasNewObject(instanceId(1,DummyUserDetails))
                  .hasReferenceChangeAt("dummyUserDetails",null,instanceId(1,DummyUserDetails))
    }

    def "should create properties snapshot of NewObject only when configured"() {
        given:
        def javers = JaversBuilder.javers().withNewObjectsSnapshot(true).build()
        def left =  new DummyUser(name: "kazik")
        def right = new DummyUser(name: "kazik", dummyUserDetails: new DummyUserDetails(id: 1, someValue: "some"))

        when:
        def diff = javers.compare(left, right)

        then:
        DiffAssert.assertThat(diff)
                .hasNewObject(instanceId(1,DummyUserDetails))
                .hasValueChangeAt("id",null,1)
                .hasValueChangeAt("someValue",null,"some")
    }

    def "should create valueChange with Enum" () {
        given:
        def user =  dummyUser("id").withSex(FEMALE).build();
        def user2 = dummyUser("id").withSex(MALE).build();
        def javers = JaversTestBuilder.newInstance()

        when:
        def diff = javers.compare(user, user2)

        then:
        diff.changes.size() == 1
        def change = diff.changes[0]
        change.left == FEMALE
        change.right == MALE
    }

    def "should serialize whole Diff"() {
        given:
        def user =  dummyUser("id").withSex(FEMALE).build();
        def user2 = dummyUser("id").withSex(MALE).withDetails(1).build();
        def javers = JaversTestBuilder.newInstance()

        when:
        def diff = javers.compare(user, user2)
        def jsonText = javers.toJson(diff)
        //println("jsonText:\n"+jsonText)

        then:
        def json = new JsonSlurper().parseText(jsonText)
        json.changes.size() == 3
        json.changes[0].changeType == "NewObject"
        json.changes[1].changeType == "ValueChange"
        json.changes[2].changeType == "ReferenceChange"
    }

    def "should support custom JsonTypeAdapter for ValueChange"() {
        given:
        def javers = javers().registerValueTypeAdapter( new DummyPointJsonTypeAdapter() )
                             .build()

        when:
        def diff = javers.compare(userWithPoint(1,2), userWithPoint(1,3))
        def jsonText = javers.toJson(diff)
        //println("jsonText:\n"+jsonText)

        then:
        def json = new JsonSlurper().parseText(jsonText)
        def change = json.changes[0];
        change.globalId.valueObject == "org.javers.core.model.DummyUserWithPoint"
        change.globalId.cdoId == "/"
        change.changeType == "ValueChange"
        change.property == "point"
        change.left == "1,2" //this is most important in this test
        change.right == "1,3" //this is most important in this test
    }

    def "should support custom native Gson TypeAdapter"() {
        given:
        def javers = javers()
                .registerValueGsonTypeAdapter(DummyPoint, new DummyPointNativeTypeAdapter() )
                .build()

        when:
        def diff = javers.compare(userWithPoint(1,2), userWithPoint(1,3))
        def jsonText = javers.toJson(diff)
        //println("jsonText:\n"+jsonText)

        then:
        def json = new JsonSlurper().parseText(jsonText)
        json.changes[0].left == "1,2"
        json.changes[0].right == "1,3"
    }

    def "should understand primitive default values when creating NewObject snapshot"() {
        given:
        def javers = javers().build()

        when:
        def diff = javers.initial(new PrimitiveEntity())

        then:
        DiffAssert.assertThat(diff).hasOnly(1, NewObject)
    }

    def "should understand primitive default values when creating ValueChange"() {
        given:
        def javers = javers().build()

        when:
        def diff = javers.compare(new PrimitiveEntity(), new PrimitiveEntity())

        then:
        DiffAssert.assertThat(diff).hasChanges(0)
    }
}
