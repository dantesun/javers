package org.javers.core;

import org.javers.common.collections.Optional;
import org.javers.core.changelog.ChangeProcessor;
import org.javers.core.commit.Commit;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.json.JsonConverter;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.GlobalId;
import org.javers.core.metamodel.object.GlobalIdDTO;
import org.javers.repository.api.JaversRepository;

import java.util.List;


/**
 * Facade to JaVers instance.<br>
 * Should be constructed by {@link JaversBuilder} provided with your domain model configuration.
 * <br/><br/>
 *
 * For example, to deeply compare two objects
 * or two arbitrary complex graphs of objects, call:
 * <pre>
 * Javers javers = JaversBuilder.javers().build();
 * Diff diff = javers.compare(oldVersion, currentVersion);
 * </pre>
 *
 * @see <a href="http://javers.org/documentation"/>http://javers.org/documentation</a>
 * @author bartosz walacik
 */
public interface Javers {

    /**
     * Persists a current state of a given domain object graph
     * in JaVers repository.
     * <br/><br/>
     * 
     * JaVers applies commit() to given object and all objects navigable from it.
     * You can capture a state of an arbitrary complex object graph with a single commit() call.
     *
     * @see <a href="http://javers.org/documentation/repository-examples/">http://javers.org/documentation/repository-examples</a>
     * @param currentVersion standalone object or handle to an object graph
     */
    Commit commit(String author, Object currentVersion);

    /**
     * Marks given object as deleted.
     * <br/><br/>
     * 
     * Unlike {@link Javers#commit(String, Object)}, this method is shallow
     * and affects only given object.
     * <br/><br/>
     *
     * This method doesn't delete anything from JaVers repository.
     * It just persists 'terminal snapshot' of given object.
     *
     * @param deleted object to be marked as deleted
     */
    Commit commitShallowDelete(String author, Object deleted);

    /**
     * The same like {@link #commitShallowDelete(String,Object)}
     * but deleted object is selected using globalId
     */
    Commit commitShallowDeleteById(String author, GlobalIdDTO globalId);

    /**
     * <p>
     * JaVers core function,
     * deeply compares two arbitrary complex object graphs.
     * </p>
     *
     * <p>
     * To calculate a diff, just provide two versions of the
     * same object or handles to two versions of the same object graph.
     * <br/>
     * The handle could be a root of an aggregate, tree root
     * or any node in an object graph from where all other nodes are navigable.
     * </p>
     *
     * <p>
     * This function is used for ad-hoc objects comparing.
     * In order to use data auditing feature, call {@link #commit(String, Object)}.
     * </p>
     *
     * <p>
     * Diffs can be converted to JSON with {@link #toJson(Diff)}.
     * </p>
     *
     * @see <a href="http://javers.org/documentation/diff-examples/">http://javers.org/documentation/diff-examples</a>
     */
    Diff compare(Object oldVersion, Object currentVersion);

    /**
     * Initial diff is a kind of snapshot of given domain object graph.
     * Use it alongside with {@link #compare(Object, Object)}
     */
    Diff initial(Object newDomainObject);

    /**
     * use:
     * <pre>
     * javers.getJsonConverter().toJson(diff);
     * </pre>
     */
    @Deprecated
    String toJson(Diff diff);

    /**
     * Snapshots (historical versions) of given object,
     * in reverse chronological order.
     * <br/><br/>
     *
     * For example, to list 5 last snapshots of "bob" Person, call:
     * <pre>
     * javers.getStateHistory(InstanceIdDTO.instanceId("bob", Person.class), 5);
     * </pre>
     *
     * @param globalId given object ID
     * @param limit choose reasonable limit
     * @return empty List if object is not versioned
     */
    List<CdoSnapshot> getStateHistory(GlobalIdDTO globalId, int limit);

    /**
     * Latest snapshot of given object
     * or Optional#EMPTY if object is not versioned.
     * <br/><br/>
     *
     * For example, to get last snapshot of "bob" Person, call:
     * <pre>
     * javers.getStateHistory(InstanceIdDTO.instanceId("bob", Person.class), 5);
     * </pre>
     */
    Optional<CdoSnapshot> getLatestSnapshot(GlobalIdDTO globalId);

    /**
     * Changes history (diff sequence) of given object,
     * in reverse chronological order.
     *
     * For example, to get change history of "bob" Person, call:
     * <pre>
     * javers.getChangeHistory(InstanceIdDTO.instanceId("bob", Person.class), 5);
     * </pre>
     *
     * @param globalId given object ID
     * @param limit choose reasonable limit
     * @return empty List, if object is not versioned or was committed only once
     */
    List<Change> getChangeHistory(GlobalIdDTO globalId, int limit);

    /**
     * If you are serializing JaVers objects like
     * {@link Commit}, {@link Change}, {@link Diff} or {@link CdoSnapshot} to JSON, use this JsonConverter.
     * For example:
     *
     * <pre>
     * javers.getJsonConverter().toJson(changes);
     * </pre>
     */
    JsonConverter getJsonConverter();

    /**
     * Generic purpose method for processing a changes list.
     * After iterating over given list, returns data computed by
     * {@link org.javers.core.changelog.ChangeProcessor#result()}.
     * <br/>
     * It's more convenient than iterating over changes on your own.
     * ChangeProcessor frees you from <tt>if + inctanceof</tt> boilerplate.
     *
     * <br/><br/>
     * Additional features: <br/>
     *  - when several changes in a row refers to the same Commit, {@link ChangeProcessor#onCommit(CommitMetadata)}
     *  is called only for first occurrence <br/>
     *  - similarly, when several changes in a row affects the same object, {@link ChangeProcessor#onAffectedObject(GlobalId)}
     *  is called only for first occurrence
     *
     * <br/><br/>
     * For example, to get pretty change log, call:
     * <pre>
     * List&lt;Change&gt; changes = javers.getChangeHistory(...);
     * String changeLog = javers.processChangeList(changes, new SimpleTextChangeLog());
     * System.out.println( changeLog );
     * </pre>
     *
     * @see org.javers.core.changelog.SimpleTextChangeLog
     */
    <T> T processChangeList(List<Change> changes, ChangeProcessor<T> changeProcessor);

    IdBuilder idBuilder();
}
