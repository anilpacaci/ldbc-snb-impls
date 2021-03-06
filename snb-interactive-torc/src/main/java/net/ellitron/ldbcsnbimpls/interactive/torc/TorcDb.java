/* 
 * Copyright (C) 2015-2016 Stanford University
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.ellitron.ldbcsnbimpls.interactive.torc;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.count;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.id;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.select;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.unfold;
import static org.apache.tinkerpop.gremlin.process.traversal.Order.incr;
import static org.apache.tinkerpop.gremlin.process.traversal.P.lt;
import static org.apache.tinkerpop.gremlin.process.traversal.P.without;

import net.ellitron.torc.util.UInt128;

import com.ldbc.driver.control.LoggingService;
import com.ldbc.driver.Db;
import com.ldbc.driver.DbConnectionState;
import com.ldbc.driver.DbException;
import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.ResultReporter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcNoResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery1;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery10;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery11;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery12;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery13;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery14;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery1Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery2;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery3;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery4;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery5;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery6;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery7;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery8;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery9;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery1PersonProfile;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery1PersonProfileResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery2PersonPosts;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery2PersonPostsResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery3PersonFriends;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery3PersonFriendsResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery4MessageContent;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery4MessageContentResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery5MessageCreator;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery5MessageCreatorResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery6MessageForum;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery6MessageForumResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery7MessageReplies;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery7MessageRepliesResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate1AddPerson;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate2AddPostLike;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate3AddCommentLike;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate4AddForum;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate5AddForumMembership;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate6AddPost;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate7AddComment;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate8AddFriendship;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the LDBC SNB interactive workload[1] for TorcDB.
 * Queries are executed against a running RAMCloud cluster. Configuration
 * parameters for this implementation (that are supplied via the LDBC driver)
 * are listed below.
 * <p>
 * Configuration Parameters:
 * <ul>
 * <li>coordinatorLocator - locator string for the RAMCloud cluster coordinator
 * (default: tcp:host=127.0.0.1,port=12246).</li>
 * <li>graphName - name of the graph stored in RAMCloud against which to
 * execute queries (default: default).</li>
 * <li>txReads - the presence of this switch turns on performing transactions
 * for read queries (Note: at time of writing complex read queries touch too
 * much data and trying to do these transactionally will result in a timeout.
 * This is currently being fixed in RAMCloud).</li>
 * </ul>
 * <p>
 * References:<br>
 * [1]: Prat, Arnau (UPC) and Boncz, Peter (VUA) and Larriba, Josep Lluís (UPC)
 * and Angles, Renzo (TALCA) and Averbuch, Alex (NEO) and Erling, Orri (OGL)
 * and Gubichev, Andrey (TUM) and Spasić, Mirko (OGL) and Pham, Minh-Duc (VUA)
 * and Martínez, Norbert (SPARSITY). "LDBC Social Network Benchmark (SNB) -
 * v0.2.2 First Public Draft Release". http://www.ldbcouncil.org/.
 * <p>
 * TODO:<br>
 * <ul>
 * </ul>
 * <p>
 *
 * @author Jonathan Ellithorpe (jde@cs.stanford.edu)
 */
public class TorcDb extends Db {

  private TorcDbConnectionState connectionState = null;
  private static boolean doTransactionalReads = false;

  // Maximum number of times to try a transaction before giving up.
  private static int MAX_TX_ATTEMPTS = 100;

  @Override
  protected void onInit(Map<String, String> properties,
      LoggingService loggingService) throws DbException {

    connectionState = new TorcDbConnectionState(properties);

    if (properties.containsKey("txReads")) {
      doTransactionalReads = true;
    }

    /*
     * Register operation handlers with the benchmark.
     */
    registerOperationHandler(LdbcQuery1.class,
        LdbcQuery1Handler.class);

    registerOperationHandler(LdbcShortQuery1PersonProfile.class,
        LdbcShortQuery1PersonProfileHandler.class);
    registerOperationHandler(LdbcShortQuery2PersonPosts.class,
        LdbcShortQuery2PersonPostsHandler.class);
    registerOperationHandler(LdbcShortQuery3PersonFriends.class,
        LdbcShortQuery3PersonFriendsHandler.class);
    registerOperationHandler(LdbcShortQuery4MessageContent.class,
        LdbcShortQuery4MessageContentHandler.class);
    registerOperationHandler(LdbcShortQuery5MessageCreator.class,
        LdbcShortQuery5MessageCreatorHandler.class);
    registerOperationHandler(LdbcShortQuery6MessageForum.class,
        LdbcShortQuery6MessageForumHandler.class);
    registerOperationHandler(LdbcShortQuery7MessageReplies.class,
        LdbcShortQuery7MessageRepliesHandler.class);

    registerOperationHandler(LdbcUpdate1AddPerson.class,
        LdbcUpdate1AddPersonHandler.class);
    registerOperationHandler(LdbcUpdate2AddPostLike.class,
        LdbcUpdate2AddPostLikeHandler.class);
    registerOperationHandler(LdbcUpdate3AddCommentLike.class,
        LdbcUpdate3AddCommentLikeHandler.class);
    registerOperationHandler(LdbcUpdate4AddForum.class,
        LdbcUpdate4AddForumHandler.class);
    registerOperationHandler(LdbcUpdate5AddForumMembership.class,
        LdbcUpdate5AddForumMembershipHandler.class);
    registerOperationHandler(LdbcUpdate6AddPost.class,
        LdbcUpdate6AddPostHandler.class);
    registerOperationHandler(LdbcUpdate7AddComment.class,
        LdbcUpdate7AddCommentHandler.class);
    registerOperationHandler(LdbcUpdate8AddFriendship.class,
        LdbcUpdate8AddFriendshipHandler.class);
  }

  @Override
  protected void onClose() throws IOException {
    connectionState.close();
  }

  @Override
  protected DbConnectionState getConnectionState() throws DbException {
    return connectionState;
  }

  /**
   * ------------------------------------------------------------------------
   * Complex Queries
   * ------------------------------------------------------------------------
   */
  /**
   * Given a start Person, find up to 20 Persons with a given first name that
   * the start Person is connected to (excluding start Person) by at most 3
   * steps via Knows relationships. Return Persons, including summaries of the
   * Persons workplaces and places of study. Sort results ascending by their
   * distance from the start Person, for Persons within the same distance sort
   * ascending by their last name, and for Persons with same last name
   * ascending by their identifier.[1]
   */
  public static class LdbcQuery1Handler
      implements OperationHandler<LdbcQuery1, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery1Handler.class);

    @Override
    public void executeOperation(final LdbcQuery1 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {
      executeOperationGremlin2(operation, dbConnectionState, resultReporter);
    }

    public void executeOperationGremlin2(final LdbcQuery1 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        long personId = operation.personId();
        String firstName = operation.firstName();
        int resultLimit = operation.limit();
        int maxLevels = 3;
        Graph graph = ((TorcDbConnectionState) dbConnectionState).getClient();

        GraphTraversalSource g = graph.traversal();

        List<Long> distList = new ArrayList<>(resultLimit);
        List<Vertex> matchList = new ArrayList<>(resultLimit);

        Vertex root = g.V(new UInt128(TorcEntity.PERSON.idSpace, personId))
            .next();

        g.withSideEffect("x", matchList).withSideEffect("d", distList)
            .V(root).aggregate("done").out("knows")
            .where(without("done")).dedup().fold().sideEffect(
                unfold().has("firstName", firstName).order()
                .by("lastName", incr).by(id(), incr).limit(resultLimit)
                .as("person")
                .select("x").by(count(Scope.local)).is(lt(resultLimit))
                .store("x").by(select("person"))
            ).filter(select("x").count(Scope.local).is(lt(resultLimit))
                .store("d")).unfold().aggregate("done").out("knows")
            .where(without("done")).dedup().fold().sideEffect(
                unfold().has("firstName", firstName).order()
                .by("lastName", incr).by(id(), incr).limit(resultLimit)
                .as("person")
                .select("x").by(count(Scope.local)).is(lt(resultLimit))
                .store("x").by(select("person"))
            ).filter(select("x").count(Scope.local).is(lt(resultLimit))
                .store("d")).unfold().aggregate("done").out("knows")
            .where(without("done")).dedup().fold().sideEffect(
                unfold().has("firstName", firstName).order()
                .by("lastName", incr).by(id(), incr).limit(resultLimit)
                .as("person")
                .select("x").by(count(Scope.local)).is(lt(resultLimit))
                .store("x").by(select("person"))
            ).select("x").count(Scope.local)
            .store("d").iterate();

        Map<Vertex, Map<String, List<String>>> propertiesMap =
            new HashMap<>(matchList.size());
        g.V(matchList.toArray()).as("person")
            .<List<String>>valueMap().as("props")
            .select("person", "props")
            .forEachRemaining(map -> {
              propertiesMap.put((Vertex) map.get("person"),
                  (Map<String, List<String>>) map.get("props"));
            });

        Map<Vertex, String> placeNameMap = new HashMap<>(matchList.size());
        g.V(matchList.toArray()).as("person")
            .out("isLocatedIn")
            .<String>values("name")
            .as("placeName")
            .select("person", "placeName")
            .forEachRemaining(map -> {
              placeNameMap.put((Vertex) map.get("person"),
                  (String) map.get("placeName"));
            });

        Map<Vertex, List<List<Object>>> universityInfoMap =
            new HashMap<>(matchList.size());
        g.V(matchList.toArray()).as("person")
            .outE("studyAt").as("classYear")
            .inV().as("universityName")
            .out("isLocatedIn").as("cityName")
            .select("person", "universityName", "classYear", "cityName")
            .by().by("name").by("classYear").by("name")
            .forEachRemaining(map -> {
              Vertex v = (Vertex) map.get("person");
              List<Object> tuple = new ArrayList<>(3);
              tuple.add(map.get("universityName"));
              tuple.add(Integer.decode((String) map.get("classYear")));
              tuple.add(map.get("cityName"));
              if (universityInfoMap.containsKey(v)) {
                universityInfoMap.get(v).add(tuple);
              } else {
                List<List<Object>> tupleList = new ArrayList<>();
                tupleList.add(tuple);
                universityInfoMap.put(v, tupleList);
              }
            });

        Map<Vertex, List<List<Object>>> companyInfoMap =
            new HashMap<>(matchList.size());
        g.V(matchList.toArray()).as("person")
            .outE("workAt").as("workFrom")
            .inV().as("companyName")
            .out("isLocatedIn").as("cityName")
            .select("person", "companyName", "workFrom", "cityName")
            .by().by("name").by("workFrom").by("name")
            .forEachRemaining(map -> {
              Vertex v = (Vertex) map.get("person");
              List<Object> tuple = new ArrayList<>(3);
              tuple.add(map.get("companyName"));
              tuple.add(Integer.decode((String) map.get("workFrom")));
              tuple.add(map.get("cityName"));
              if (companyInfoMap.containsKey(v)) {
                companyInfoMap.get(v).add(tuple);
              } else {
                List<List<Object>> tupleList = new ArrayList<>();
                tupleList.add(tuple);
                companyInfoMap.put(v, tupleList);
              }
            });

        List<LdbcQuery1Result> result = new ArrayList<>();

        for (int i = 0; i < matchList.size(); i++) {
          Vertex match = matchList.get(i);
          int distance = (i < distList.get(0)) ? 1
              : (i < distList.get(1)) ? 2 : 3;
          Map<String, List<String>> properties = propertiesMap.get(match);
          List<String> emails = properties.get("email");
          if (emails == null) {
            emails = new ArrayList<>();
          }
          List<String> languages = properties.get("language");
          if (languages == null) {
            languages = new ArrayList<>();
          }
          String placeName = placeNameMap.get(match);
          List<List<Object>> universityInfo = universityInfoMap.get(match);
          if (universityInfo == null) {
            universityInfo = new ArrayList<>();
          }
          List<List<Object>> companyInfo = companyInfoMap.get(match);
          if (companyInfo == null) {
            companyInfo = new ArrayList<>();
          }
          result.add(new LdbcQuery1Result(
              ((UInt128) match.id()).getLowerLong(),
              properties.get("lastName").get(0),
              distance,
              Long.decode(properties.get("birthday").get(0)),
              Long.decode(properties.get("creationDate").get(0)),
              properties.get("gender").get(0),
              properties.get("browserUsed").get(0),
              properties.get("locationIP").get(0),
              emails,
              languages,
              placeName,
              universityInfo,
              companyInfo));
        }

        if (doTransactionalReads) {
          try {
            graph.tx().commit();
          } catch (RuntimeException e) {
            txAttempts++;
            continue;
          }
        } else {
          graph.tx().rollback();
        }

        resultReporter.report(result.size(), result, operation);
        break;
      }
    }

    public void executeOperationGremlin1(final LdbcQuery1 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        long personId = operation.personId();
        String firstName = operation.firstName();
        int resultLimit = operation.limit();
        int maxLevels = 3;
        Graph graph = ((TorcDbConnectionState) dbConnectionState).getClient();

        GraphTraversalSource g = graph.traversal();

        List<Integer> distList = new ArrayList<>(resultLimit);
        List<Vertex> matchList = new ArrayList<>(resultLimit);

        Vertex root = g.V(new UInt128(TorcEntity.PERSON.idSpace, personId))
            .next();

        List<Vertex> l1Friends = new ArrayList<>();
        g.V(root).out("knows")
            .sideEffect(v -> l1Friends.add(v.get()))
            .has("firstName", firstName)
            .order()
            .by("lastName", incr)
            .by(id(), incr)
            .sideEffect(v -> {
              if (matchList.size() < resultLimit) {
                matchList.add(v.get());
                distList.add(1);
              }
            })
            .iterate();

        if (matchList.size() < resultLimit && l1Friends.size() > 0) {
          List<Vertex> l2Friends = new ArrayList<>();
          g.V(l1Friends.toArray())
              .out("knows")
              .dedup()
              .is(without(l1Friends))
              .is(without(root))
              .sideEffect(v -> l2Friends.add(v.get()))
              .has("firstName", firstName)
              .order()
              .by("lastName", incr)
              .by(id(), incr)
              .sideEffect(v -> {
                if (matchList.size() < resultLimit) {
                  matchList.add(v.get());
                  distList.add(2);
                }
              })
              .iterate();

          if (matchList.size() < resultLimit && l2Friends.size() > 0) {
            g.V(l2Friends.toArray())
                .out("knows")
                .dedup()
                .is(without(l2Friends))
                .is(without(l1Friends))
                .has("firstName", firstName)
                .order()
                .by("lastName", incr)
                .by(id(), incr)
                .sideEffect(v -> {
                  if (matchList.size() < resultLimit) {
                    matchList.add(v.get());
                    distList.add(3);
                  }
                })
                .iterate();
          }
        }

        Map<Vertex, Map<String, List<String>>> propertiesMap =
            new HashMap<>(matchList.size());
        g.V(matchList.toArray()).as("person")
            .<List<String>>valueMap().as("props")
            .select("person", "props")
            .forEachRemaining(map -> {
              propertiesMap.put((Vertex) map.get("person"),
                  (Map<String, List<String>>) map.get("props"));
            });

        Map<Vertex, String> placeNameMap = new HashMap<>(matchList.size());
        g.V(matchList.toArray()).as("person")
            .out("isLocatedIn")
            .<String>values("name")
            .as("placeName")
            .select("person", "placeName")
            .forEachRemaining(map -> {
              placeNameMap.put((Vertex) map.get("person"),
                  (String) map.get("placeName"));
            });

        Map<Vertex, List<List<Object>>> universityInfoMap =
            new HashMap<>(matchList.size());
        g.V(matchList.toArray()).as("person")
            .outE("studyAt").as("classYear")
            .inV().as("universityName")
            .out("isLocatedIn").as("cityName")
            .select("person", "universityName", "classYear", "cityName")
            .by().by("name").by("classYear").by("name")
            .forEachRemaining(map -> {
              Vertex v = (Vertex) map.get("person");
              List<Object> tuple = new ArrayList<>(3);
              tuple.add(map.get("universityName"));
              tuple.add(Integer.decode((String) map.get("classYear")));
              tuple.add(map.get("cityName"));
              if (universityInfoMap.containsKey(v)) {
                universityInfoMap.get(v).add(tuple);
              } else {
                List<List<Object>> tupleList = new ArrayList<>();
                tupleList.add(tuple);
                universityInfoMap.put(v, tupleList);
              }
            });

        Map<Vertex, List<List<Object>>> companyInfoMap =
            new HashMap<>(matchList.size());
        g.V(matchList.toArray()).as("person")
            .outE("workAt").as("workFrom")
            .inV().as("companyName")
            .out("isLocatedIn").as("cityName")
            .select("person", "companyName", "workFrom", "cityName")
            .by().by("name").by("workFrom").by("name")
            .forEachRemaining(map -> {
              Vertex v = (Vertex) map.get("person");
              List<Object> tuple = new ArrayList<>(3);
              tuple.add(map.get("companyName"));
              tuple.add(Integer.decode((String) map.get("workFrom")));
              tuple.add(map.get("cityName"));
              if (companyInfoMap.containsKey(v)) {
                companyInfoMap.get(v).add(tuple);
              } else {
                List<List<Object>> tupleList = new ArrayList<>();
                tupleList.add(tuple);
                companyInfoMap.put(v, tupleList);
              }
            });

        List<LdbcQuery1Result> result = new ArrayList<>();

        for (int i = 0; i < matchList.size(); i++) {
          Vertex match = matchList.get(i);
          Map<String, List<String>> properties = propertiesMap.get(match);
          List<String> emails = properties.get("email");
          if (emails == null) {
            emails = new ArrayList<>();
          }
          List<String> languages = properties.get("language");
          if (languages == null) {
            languages = new ArrayList<>();
          }
          String placeName = placeNameMap.get(match);
          List<List<Object>> universityInfo = universityInfoMap.get(match);
          if (universityInfo == null) {
            universityInfo = new ArrayList<>();
          }
          List<List<Object>> companyInfo = companyInfoMap.get(match);
          if (companyInfo == null) {
            companyInfo = new ArrayList<>();
          }
          result.add(new LdbcQuery1Result(
              ((UInt128) match.id()).getLowerLong(),
              properties.get("lastName").get(0),
              distList.get(i),
              Long.decode(properties.get("birthday").get(0)),
              Long.decode(properties.get("creationDate").get(0)),
              properties.get("gender").get(0),
              properties.get("browserUsed").get(0),
              properties.get("locationIP").get(0),
              emails,
              languages,
              placeName,
              universityInfo,
              companyInfo));
        }

        if (doTransactionalReads) {
          try {
            graph.tx().commit();
          } catch (RuntimeException e) {
            txAttempts++;
            continue;
          }
        } else {
          graph.tx().rollback();
        }

        resultReporter.report(result.size(), result, operation);
        break;
      }
    }

    public void executeOperationRaw(final LdbcQuery1 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        long personId = operation.personId();
        String firstName = operation.firstName();
        int maxLevels = 3;

        Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

        Vertex rootPerson = client.vertices(
            new UInt128(TorcEntity.PERSON.idSpace, personId)).next();

        List<Vertex> friends = new ArrayList<>();
        List<Integer> levelIndices = new ArrayList<>();
        List<Integer> matchIndices = new ArrayList<>();

        friends.add(rootPerson);
        levelIndices.add(friends.size());
        int currentLevel = 0;

        for (int i = 0; i < friends.size(); i++) {
          if (i == levelIndices.get(levelIndices.size() - 1)) {
            levelIndices.add(friends.size());
            currentLevel++;

            if (currentLevel == maxLevels) {
              break;
            }

            if (matchIndices.size() >= operation.limit()) {
              break;
            }
          }

          Vertex person = friends.get(i);

          person.edges(Direction.OUT, "knows").forEachRemaining((e) -> {
            Vertex friend = e.inVertex();
            if (!friends.contains(friend)) {
              friends.add(friend);
              if (friend.<String>property("firstName").value()
                  .equals(firstName)) {
                matchIndices.add(friends.size() - 1);
              }
            }
          });
        }

        List<LdbcQuery1Result> result = new ArrayList<>();

        int matchNumber = 0;
        for (int level = 1; level < levelIndices.size(); level++) {
          int endIndex = levelIndices.get(level);

          List<Vertex> equidistantVertices = new ArrayList<>();
          while (matchNumber < matchIndices.size()
              && matchIndices.get(matchNumber) < endIndex) {
            Vertex friend = friends.get(matchIndices.get(matchNumber));
            equidistantVertices.add(friend);
            matchNumber++;
          }

          equidistantVertices.sort((a, b) -> {
            Vertex v1 = (Vertex) a;
            Vertex v2 = (Vertex) b;

            String v1LastName = v1.<String>property("lastName").value();
            String v2LastName = v2.<String>property("lastName").value();

            int lastNameCompareVal = v1LastName.compareTo(v2LastName);
            if (lastNameCompareVal != 0) {
              return lastNameCompareVal;
            } else {
              UInt128 v1Id = (UInt128) v1.id();
              UInt128 v2Id = (UInt128) v2.id();

              return v1Id.compareTo(v2Id);
            }
          });

          for (Vertex f : equidistantVertices) {
            long friendId = ((UInt128) f.id()).getLowerLong();
            String friendLastName = null;
            int distanceFromPerson = level;
            long friendBirthday = 0;
            long friendCreationDate = 0;
            String friendGender = null;
            String friendBrowserUsed = null;
            String friendLocationIp = null;
            List<String> friendEmails = new ArrayList<>();
            List<String> friendLanguages = new ArrayList<>();
            String friendCityName = null;
            List<List<Object>> friendUniversities = new ArrayList<>();
            List<List<Object>> friendCompanies = new ArrayList<>();

            // Extract normal properties.
            Iterator<VertexProperty<String>> props = f.properties();
            while (props.hasNext()) {
              VertexProperty<String> prop = props.next();

              switch (prop.key()) {
                case "lastName":
                  friendLastName = prop.value();
                  break;
                case "birthday":
                  friendBirthday = Long.decode(prop.value());
                  break;
                case "creationDate":
                  friendCreationDate = Long.decode(prop.value());
                  break;
                case "gender":
                  friendGender = prop.value();
                  break;
                case "browserUsed":
                  friendBrowserUsed = prop.value();
                  break;
                case "locationIP":
                  friendLocationIp = prop.value();
                  break;
                case "email":
                  friendEmails.add(prop.value());
                  break;
                case "language":
                  friendLanguages.add(prop.value());
                  break;
              }
            }

            // Fetch where person is located
            Vertex friendPlace =
                f.edges(Direction.OUT, "isLocatedIn").next().inVertex();
            friendCityName = friendPlace.<String>property("name").value();

            // Fetch universities studied at
            f.edges(Direction.OUT, "studyAt").forEachRemaining((e) -> {
              Integer classYear =
                  Integer.decode(e.<String>property("classYear").value());
              Vertex organization = e.inVertex();
              String orgName = organization.<String>property("name").value();
              Vertex place = organization.edges(Direction.OUT, "isLocatedIn")
                  .next().inVertex();
              String placeName = place.<String>property("name").value();

              List<Object> universityInfo = new ArrayList<>();
              universityInfo.add(orgName);
              universityInfo.add(classYear);
              universityInfo.add(placeName);

              friendUniversities.add(universityInfo);
            });

            // Fetch companies worked at
            f.edges(Direction.OUT, "workAt").forEachRemaining((e) -> {
              Integer workFrom =
                  Integer.decode(e.<String>property("workFrom").value());
              Vertex company = e.inVertex();
              String compName = company.<String>property("name").value();
              Vertex place = company.edges(Direction.OUT, "isLocatedIn")
                  .next().inVertex();
              String placeName = place.<String>property("name").value();

              List<Object> companyInfo = new ArrayList<>();
              companyInfo.add(compName);
              companyInfo.add(workFrom);
              companyInfo.add(placeName);

              friendCompanies.add(companyInfo);
            });

            LdbcQuery1Result res = new LdbcQuery1Result(
                friendId,
                friendLastName,
                level,
                friendBirthday,
                friendCreationDate,
                friendGender,
                friendBrowserUsed,
                friendLocationIp,
                friendEmails,
                friendLanguages,
                friendCityName,
                friendUniversities,
                friendCompanies
            );

            result.add(res);

            if (result.size() == operation.limit()) {
              break;
            }
          }

          if (result.size() == operation.limit()
              || matchNumber == matchIndices.size()) {
            break;
          }
        }

        if (doTransactionalReads) {
          try {
            client.tx().commit();
          } catch (RuntimeException e) {
            txAttempts++;
            continue;
          }
        } else {
          client.tx().rollback();
        }

        resultReporter.report(result.size(), result, operation);
        break;
      }
    }

  }

  /**
   * Given a start Person, find (most recent) Posts and Comments from all of
   * that Person’s friends, that were created before (and including) a given
   * date. Return the top 20 Posts/Comments, and the Person that created each
   * of them. Sort results descending by creation date, and then ascending by
   * Post identifier.[1]
   */
  public static class LdbcQuery2Handler
      implements OperationHandler<LdbcQuery2, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery2Handler.class);

    @Override
    public void executeOperation(final LdbcQuery2 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given a start Person, find Persons that are their friends and friends of
   * friends (excluding start Person) that have made Posts/Comments in both of
   * the given Countries, X and Y, within a given period. Only Persons that are
   * foreign to Countries X and Y are considered, that is Persons whose
   * Location is not Country X or Country Y. Return top 20 Persons, and their
   * Post/Comment counts, in the given countries and period. Sort results
   * descending by total number of Posts/Comments, and then ascending by Person
   * identifier.[1]
   */
  public static class LdbcQuery3Handler
      implements OperationHandler<LdbcQuery3, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery3Handler.class);

    @Override
    public void executeOperation(final LdbcQuery3 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given a start Person, find Tags that are attached to Posts that were
   * created by that Person’s friends. Only include Tags that were attached to
   * friends’ Posts created within a given time interval, and that were never
   * attached to friends’ Posts created before this interval. Return top 10
   * Tags, and the count of Posts, which were created within the given time
   * interval, that this Tag was attached to. Sort results descending by Post
   * count, and then ascending by Tag name.[1]
   */
  public static class LdbcQuery4Handler
      implements OperationHandler<LdbcQuery4, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery4Handler.class);

    @Override
    public void executeOperation(final LdbcQuery4 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given a start Person, find the Forums which that Person’s friends and
   * friends of friends (excluding start Person) became Members of after a
   * given date. Return top 20 Forums, and the number of Posts in each Forum
   * that was Created by any of these Persons. For each Forum consider only
   * those Persons which joined that particular Forum after the given date.
   * Sort results descending by the count of Posts, and then ascending by Forum
   * identifier.[1]
   */
  public static class LdbcQuery5Handler
      implements OperationHandler<LdbcQuery5, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery5Handler.class);

    @Override
    public void executeOperation(final LdbcQuery5 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given a start Person and some Tag, find the other Tags that occur together
   * with this Tag on Posts that were created by start Person’s friends and
   * friends of friends (excluding start Person). Return top 10 Tags, and the
   * count of Posts that were created by these Persons, which contain both this
   * Tag and the given Tag. Sort results descending by count, and then
   * ascending by Tag name.[1]
   */
  public static class LdbcQuery6Handler
      implements OperationHandler<LdbcQuery6, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery6Handler.class);

    @Override
    public void executeOperation(final LdbcQuery6 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given a start Person, find (most recent) Likes on any of start Person’s
   * Posts/Comments. Return top 20 Persons that Liked any of start Person’s
   * Posts/Comments, the Post/Comment they liked most recently, creation date
   * of that Like, and the latency (in minutes) between creation of
   * Post/Comment and Like. Additionally, return a flag indicating whether the
   * liker is a friend of start Person. In the case that a Person Liked
   * multiple Posts/Comments at the same time, return the Post/Comment with
   * lowest identifier. Sort results descending by creation time of Like, then
   * ascending by Person identifier of liker.[1]
   */
  public static class LdbcQuery7Handler
      implements OperationHandler<LdbcQuery7, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery7Handler.class);

    @Override
    public void executeOperation(final LdbcQuery7 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given a start Person, find (most recent) Comments that are replies to
   * Posts/Comments of the start Person. Only consider immediate (1-hop)
   * replies, not the transitive (multi-hop) case. Return the top 20 reply
   * Comments, and the Person that created each reply Comment. Sort results
   * descending by creation date of reply Comment, and then ascending by
   * identifier of reply Comment.[1]
   */
  public static class LdbcQuery8Handler
      implements OperationHandler<LdbcQuery8, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery8Handler.class);

    @Override
    public void executeOperation(final LdbcQuery8 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given a start Person, find the (most recent) Posts/Comments created by
   * that Person’s friends or friends of friends (excluding start Person). Only
   * consider the Posts/Comments created before a given date (excluding that
   * date). Return the top 20 Posts/Comments, and the Person that created each
   * of those Posts/Comments. Sort results descending by creation date of
   * Post/Comment, and then ascending by Post/Comment identifier.[1]
   */
  public static class LdbcQuery9Handler
      implements OperationHandler<LdbcQuery9, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery9Handler.class);

    @Override
    public void executeOperation(final LdbcQuery9 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given a start Person, find that Person’s friends of friends (excluding
   * start Person, and immediate friends), who were born on or after the 21st
   * of a given month (in any year) and before the 22nd of the following month.
   * Calculate the similarity between each of these Persons and start Person,
   * where similarity for any Person is defined as follows:
   * <ul>
   * <li>common = number of Posts created by that Person, such that the Post
   * has a Tag that start Person is Interested in</li>
   * <li>uncommon = number of Posts created by that Person, such that the Post
   * has no Tag that start Person is Interested in</li>
   * <li>similarity = common - uncommon</li>
   * </ul>
   * Return top 10 Persons, their Place, and their similarity score. Sort
   * results descending by similarity score, and then ascending by Person
   * identifier.[1]
   */
  public static class LdbcQuery10Handler
      implements OperationHandler<LdbcQuery10, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery10Handler.class);

    @Override
    public void executeOperation(final LdbcQuery10 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given a start Person, find that Person’s friends and friends of friends
   * (excluding start Person) who started Working in some Company in a given
   * Country, before a given date (year). Return top 10 Persons, the Company
   * they worked at, and the year they started working at that Company. Sort
   * results ascending by the start date, then ascending by Person identifier,
   * and lastly by Organization name descending.[1]
   */
  public static class LdbcQuery11Handler
      implements OperationHandler<LdbcQuery11, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery11Handler.class);

    @Override
    public void executeOperation(final LdbcQuery11 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given a start Person, find the Comments that this Person’s friends made in
   * reply to Posts, considering only those Comments that are immediate (1-hop)
   * replies to Posts, not the transitive (multi-hop) case. Only consider Posts
   * with a Tag in a given TagClass or in a descendent of that TagClass. Count
   * the number of these reply Comments, and collect the Tags (with valid tag
   * class) that were attached to the Posts they replied to. Return top 20
   * Persons with at least one reply, the reply count, and the collection of
   * Tags. Sort results descending by Comment count, and then ascending by
   * Person identifier.[1]
   */
  public static class LdbcQuery12Handler
      implements OperationHandler<LdbcQuery12, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery12Handler.class);

    @Override
    public void executeOperation(final LdbcQuery12 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given two Persons, find the shortest path between these two Persons in the
   * subgraph induced by the Knows relationships. Return the length of this
   * path. -1 should be returned if no path is found, and 0 should be returned
   * if the start person is the same as the end person.[1]
   */
  public static class LdbcQuery13Handler
      implements OperationHandler<LdbcQuery13, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery13Handler.class);

    @Override
    public void executeOperation(final LdbcQuery13 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * Given two Persons, find all (unweighted) shortest paths between these two
   * Persons, in the subgraph induced by the Knows relationship. Then, for each
   * path calculate a weight. The nodes in the path are Persons, and the weight
   * of a path is the sum of weights between every pair of consecutive Person
   * nodes in the path. The weight for a pair of Persons is calculated such
   * that every reply (by one of the Persons) to a Post (by the other Person)
   * contributes 1.0, and every reply (by ones of the Persons) to a Comment (by
   * the other Person) contributes 0.5. Return all the paths with shortest
   * length, and their weights. Sort results descending by path weight. The
   * order of paths with the same weight is unspecified.[1]
   */
  public static class LdbcQuery14Handler
      implements OperationHandler<LdbcQuery14, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcQuery14Handler.class);

    @Override
    public void executeOperation(final LdbcQuery14 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

    }

  }

  /**
   * ------------------------------------------------------------------------
   * Short Queries
   * ------------------------------------------------------------------------
   */
  /**
   * Given a start Person, retrieve their first name, last name, birthday, IP
   * address, browser, and city of residence.[1]
   */
  public static class LdbcShortQuery1PersonProfileHandler implements
      OperationHandler<LdbcShortQuery1PersonProfile, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery1PersonProfileHandler.class);

    @Override
    public void executeOperation(final LdbcShortQuery1PersonProfile operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {
      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        long person_id = operation.personId();
        Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

        Vertex person = client.vertices(
            new UInt128(TorcEntity.PERSON.idSpace, person_id)).next();
        Iterator<VertexProperty<String>> props = person.properties();
        Map<String, String> propertyMap = new HashMap<>();
        props.forEachRemaining((prop) -> {
          propertyMap.put(prop.key(), prop.value());
        });

        Vertex place =
            person.edges(Direction.OUT, "isLocatedIn").next().inVertex();
        long placeId = ((UInt128) place.id()).getLowerLong();

        LdbcShortQuery1PersonProfileResult res =
            new LdbcShortQuery1PersonProfileResult(
                propertyMap.get("firstName"),
                propertyMap.get("lastName"),
                Long.parseLong(propertyMap.get("birthday")),
                propertyMap.get("locationIP"),
                propertyMap.get("browserUsed"),
                placeId,
                propertyMap.get("gender"),
                Long.parseLong(propertyMap.get("creationDate")));

        if (doTransactionalReads) {
          try {
            client.tx().commit();
          } catch (RuntimeException e) {
            txAttempts++;
            continue;
          }
        } else {
          client.tx().rollback();
        }

        resultReporter.report(0, res, operation);
        break;
      }
    }

  }

  /**
   * Given a start Person, retrieve the last 10 Messages (Posts or Comments)
   * created by that user. For each message, return that message, the original
   * post in its conversation, and the author of that post. If any of the
   * Messages is a Post, then the original Post will be the same Message, i.e.,
   * that Message will appear twice in that result. Order results descending by
   * message creation date, then descending by message identifier.[1]
   */
  public static class LdbcShortQuery2PersonPostsHandler implements
      OperationHandler<LdbcShortQuery2PersonPosts, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery2PersonPostsHandler.class);

    @Override
    public void executeOperation(final LdbcShortQuery2PersonPosts operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {
      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

        List<LdbcShortQuery2PersonPostsResult> result = new ArrayList<>();

        Vertex person = client.vertices(
            new UInt128(TorcEntity.PERSON.idSpace, operation.personId()))
            .next();
        Iterator<Edge> edges = person.edges(Direction.IN, "hasCreator");

        List<Vertex> messageList = new ArrayList<>();
        edges.forEachRemaining((e) -> messageList.add(e.outVertex()));
        messageList.sort((a, b) -> {
          Vertex v1 = (Vertex) a;
          Vertex v2 = (Vertex) b;

          long v1Date =
              Long.decode(v1.<String>property("creationDate").value());
          long v2Date =
              Long.decode(v2.<String>property("creationDate").value());

          if (v1Date > v2Date) {
            return -1;
          } else if (v1Date < v2Date) {
            return 1;
          } else {
            long v1Id = ((UInt128) v1.id()).getLowerLong();
            long v2Id = ((UInt128) v2.id()).getLowerLong();
            if (v1Id > v2Id) {
              return -1;
            } else if (v1Id < v2Id) {
              return 1;
            } else {
              return 0;
            }
          }
        });

        for (int i = 0; i < Integer.min(operation.limit(), messageList.size());
            i++) {
          Vertex message = messageList.get(i);

          Map<String, String> propMap = new HashMap<>();
          message.<String>properties().forEachRemaining((vp) -> {
            propMap.put(vp.key(), vp.value());
          });

          long messageId = ((UInt128) message.id()).getLowerLong();

          String messageContent;
          if (propMap.get("content").length() != 0) {
            messageContent = propMap.get("content");
          } else {
            messageContent = propMap.get("imageFile");
          }

          long messageCreationDate = Long.decode(propMap.get("creationDate"));

          long originalPostId;
          long originalPostAuthorId;
          String originalPostAuthorFirstName;
          String originalPostAuthorLastName;
          if (message.label().equals(TorcEntity.POST.label)) {
            originalPostId = messageId;
            originalPostAuthorId = ((UInt128) person.id()).getLowerLong();
            originalPostAuthorFirstName =
                person.<String>property("firstName").value();
            originalPostAuthorLastName =
                person.<String>property("lastName").value();
          } else {
            Vertex parentMessage =
                message.edges(Direction.OUT, "replyOf").next().inVertex();
            while (true) {
              if (parentMessage.label().equals(TorcEntity.POST.label)) {
                originalPostId = ((UInt128) parentMessage.id()).getLowerLong();

                Vertex author =
                    parentMessage.edges(Direction.OUT, "hasCreator")
                    .next().inVertex();
                originalPostAuthorId = ((UInt128) author.id()).getLowerLong();
                originalPostAuthorFirstName =
                    author.<String>property("firstName").value();
                originalPostAuthorLastName =
                    author.<String>property("lastName").value();
                break;
              } else {
                parentMessage =
                    parentMessage.edges(Direction.OUT, "replyOf")
                    .next().inVertex();
              }
            }
          }

          LdbcShortQuery2PersonPostsResult res =
              new LdbcShortQuery2PersonPostsResult(
                  messageId,
                  messageContent,
                  messageCreationDate,
                  originalPostId,
                  originalPostAuthorId,
                  originalPostAuthorFirstName,
                  originalPostAuthorLastName);

          result.add(res);
        }

        if (doTransactionalReads) {
          try {
            client.tx().commit();
          } catch (RuntimeException e) {
            txAttempts++;
            continue;
          }
        } else {
          client.tx().rollback();
        }

        resultReporter.report(result.size(), result, operation);
        break;
      }
    }
  }

  /**
   * Given a start Person, retrieve all of their friends, and the date at which
   * they became friends. Order results descending by friendship creation date,
   * then ascending by friend identifier.[1]
   */
  public static class LdbcShortQuery3PersonFriendsHandler implements
      OperationHandler<LdbcShortQuery3PersonFriends, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery3PersonFriendsHandler.class);

    @Override
    public void executeOperation(final LdbcShortQuery3PersonFriends operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {
      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

        List<LdbcShortQuery3PersonFriendsResult> result = new ArrayList<>();

        Vertex person = client.vertices(
            new UInt128(TorcEntity.PERSON.idSpace, operation.personId()))
            .next();

        Iterator<Edge> edges = person.edges(Direction.OUT, "knows");

        edges.forEachRemaining((e) -> {
          long creationDate = Long.decode(e.<String>property("creationDate")
              .value());

          Vertex friend = e.inVertex();

          long personId = ((UInt128) friend.id()).getLowerLong();

          String firstName = friend.<String>property("firstName").value();
          String lastName = friend.<String>property("lastName").value();

          LdbcShortQuery3PersonFriendsResult res =
              new LdbcShortQuery3PersonFriendsResult(
                  personId,
                  firstName,
                  lastName,
                  creationDate);
          result.add(res);
        });

        // Sort the result here.
        result.sort((a, b) -> {
          LdbcShortQuery3PersonFriendsResult r1 =
              (LdbcShortQuery3PersonFriendsResult) a;
          LdbcShortQuery3PersonFriendsResult r2 =
              (LdbcShortQuery3PersonFriendsResult) b;

          if (r1.friendshipCreationDate() > r2.friendshipCreationDate()) {
            return -1;
          } else if (r1.friendshipCreationDate()
              < r2.friendshipCreationDate()) {
            return 1;
          } else {
            if (r1.personId() > r2.personId()) {
              return 1;
            } else if (r1.personId() < r2.personId()) {
              return -1;
            } else {
              return 0;
            }
          }
        });

        if (doTransactionalReads) {
          try {
            client.tx().commit();
          } catch (RuntimeException e) {
            txAttempts++;
            continue;
          }
        } else {
          client.tx().rollback();
        }

        resultReporter.report(result.size(), result, operation);
        break;
      }

    }
  }

  /**
   * Given a Message (Post or Comment), retrieve its content and creation
   * date.[1]
   */
  public static class LdbcShortQuery4MessageContentHandler implements
      OperationHandler<LdbcShortQuery4MessageContent, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery4MessageContentHandler.class);

    @Override
    public void executeOperation(final LdbcShortQuery4MessageContent operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {
      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

        Vertex message = client.vertices(
            new UInt128(TorcEntity.COMMENT.idSpace, operation.messageId()))
            .next();

        long creationDate =
            Long.decode(message.<String>property("creationDate").value());
        String content = message.<String>property("content").value();
        if (content.length() == 0) {
          content = message.<String>property("imageFile").value();
        }

        LdbcShortQuery4MessageContentResult result =
            new LdbcShortQuery4MessageContentResult(
                content,
                creationDate);

        if (doTransactionalReads) {
          try {
            client.tx().commit();
          } catch (RuntimeException e) {
            txAttempts++;
            continue;
          }
        } else {
          client.tx().rollback();
        }

        resultReporter.report(1, result, operation);
        break;
      }

    }
  }

  /**
   * Given a Message (Post or Comment), retrieve its author.[1]
   */
  public static class LdbcShortQuery5MessageCreatorHandler implements
      OperationHandler<LdbcShortQuery5MessageCreator, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery5MessageCreatorHandler.class);

    @Override
    public void executeOperation(final LdbcShortQuery5MessageCreator operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {
      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

        Vertex message = client.vertices(
            new UInt128(TorcEntity.COMMENT.idSpace, operation.messageId()))
            .next();

        Vertex creator =
            message.edges(Direction.OUT, "hasCreator").next().inVertex();

        long creatorId = ((UInt128) creator.id()).getLowerLong();
        String creatorFirstName =
            creator.<String>property("firstName").value();
        String creatorLastName =
            creator.<String>property("lastName").value();

        LdbcShortQuery5MessageCreatorResult result =
            new LdbcShortQuery5MessageCreatorResult(
                creatorId,
                creatorFirstName,
                creatorLastName);

        if (doTransactionalReads) {
          try {
            client.tx().commit();
          } catch (RuntimeException e) {
            txAttempts++;
            continue;
          }
        } else {
          client.tx().rollback();
        }

        resultReporter.report(1, result, operation);
        break;
      }

    }
  }

  /**
   * Given a Message (Post or Comment), retrieve the Forum that contains it and
   * the Person that moderates that forum. Since comments are not directly
   * contained in forums, for comments, return the forum containing the
   * original post in the thread which the comment is replying to.[1]
   */
  public static class LdbcShortQuery6MessageForumHandler implements
      OperationHandler<LdbcShortQuery6MessageForum, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery6MessageForumHandler.class);

    @Override
    public void executeOperation(final LdbcShortQuery6MessageForum operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {
      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

        Vertex vertex = client.vertices(
            new UInt128(TorcEntity.COMMENT.idSpace, operation.messageId()))
            .next();

        LdbcShortQuery6MessageForumResult result;
        while (true) {
          if (vertex.label().equals(TorcEntity.FORUM.label)) {
            long forumId = ((UInt128) vertex.id()).getLowerLong();
            String forumTitle = vertex.<String>property("title").value();

            Vertex moderator =
                vertex.edges(Direction.OUT, "hasModerator").next().inVertex();

            long moderatorId = ((UInt128) moderator.id()).getLowerLong();
            String moderatorFirstName =
                moderator.<String>property("firstName").value();
            String moderatorLastName =
                moderator.<String>property("lastName").value();

            result = new LdbcShortQuery6MessageForumResult(
                forumId,
                forumTitle,
                moderatorId,
                moderatorFirstName,
                moderatorLastName);

            break;
          } else if (vertex.label().equals(TorcEntity.POST.label)) {
            vertex =
                vertex.edges(Direction.IN, "containerOf").next().outVertex();
          } else {
            vertex = vertex.edges(Direction.OUT, "replyOf").next().inVertex();
          }
        }

        if (doTransactionalReads) {
          try {
            client.tx().commit();
          } catch (RuntimeException e) {
            txAttempts++;
            continue;
          }
        } else {
          client.tx().rollback();
        }

        resultReporter.report(1, result, operation);
        break;
      }

    }
  }

  /**
   * Given a Message (Post or Comment), retrieve the (1-hop) Comments that
   * reply to it. In addition, return a boolean flag indicating if the author
   * of the reply knows the author of the original message. If author is same
   * as original author, return false for "knows" flag. Order results
   * descending by creation date, then ascending by author identifier.[1]
   */
  public static class LdbcShortQuery7MessageRepliesHandler implements
      OperationHandler<LdbcShortQuery7MessageReplies, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery7MessageRepliesHandler.class);

    @Override
    public void executeOperation(final LdbcShortQuery7MessageReplies operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {
      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

        Vertex message = client.vertices(
            new UInt128(TorcEntity.COMMENT.idSpace, operation.messageId()))
            .next();
        Vertex messageAuthor =
            message.edges(Direction.OUT, "hasCreator").next().inVertex();
        long messageAuthorId = ((UInt128) messageAuthor.id()).getLowerLong();

        List<Vertex> replies = new ArrayList<>();
        message.edges(Direction.IN, "replyOf").forEachRemaining((e) -> {
          replies.add(e.outVertex());
        });

        List<Long> messageAuthorFriendIds = new ArrayList<>();
        messageAuthor.edges(Direction.OUT, "knows").forEachRemaining((e) -> {
          messageAuthorFriendIds.add(((UInt128) e.inVertex().id())
              .getLowerLong());
        });

        List<LdbcShortQuery7MessageRepliesResult> result = new ArrayList<>();

        for (Vertex reply : replies) {
          long replyId = ((UInt128) reply.id()).getLowerLong();
          String replyContent = reply.<String>property("content").value();
          long replyCreationDate =
              Long.decode(reply.<String>property("creationDate").value());

          Vertex replyAuthor =
              reply.edges(Direction.OUT, "hasCreator").next().inVertex();
          long replyAuthorId = ((UInt128) replyAuthor.id()).getLowerLong();
          String replyAuthorFirstName =
              replyAuthor.<String>property("firstName").value();
          String replyAuthorLastName =
              replyAuthor.<String>property("lastName").value();

          boolean knows = false;
          if (messageAuthorId != replyAuthorId) {
            knows = messageAuthorFriendIds.contains(replyAuthorId);
          }

          LdbcShortQuery7MessageRepliesResult res =
              new LdbcShortQuery7MessageRepliesResult(
                  replyId,
                  replyContent,
                  replyCreationDate,
                  replyAuthorId,
                  replyAuthorFirstName,
                  replyAuthorLastName,
                  knows
              );

          result.add(res);
        }

        // Sort the result here.
        result.sort((a, b) -> {
          LdbcShortQuery7MessageRepliesResult r1 =
              (LdbcShortQuery7MessageRepliesResult) a;
          LdbcShortQuery7MessageRepliesResult r2 =
              (LdbcShortQuery7MessageRepliesResult) b;

          if (r1.commentCreationDate() > r2.commentCreationDate()) {
            return -1;
          } else if (r1.commentCreationDate() < r2.commentCreationDate()) {
            return 1;
          } else {
            if (r1.replyAuthorId() > r2.replyAuthorId()) {
              return 1;
            } else if (r1.replyAuthorId() < r2.replyAuthorId()) {
              return -1;
            } else {
              return 0;
            }
          }
        });

        if (doTransactionalReads) {
          try {
            client.tx().commit();
          } catch (RuntimeException e) {
            txAttempts++;
            continue;
          }
        } else {
          client.tx().rollback();
        }

        resultReporter.report(result.size(), result, operation);
        break;
      }
    }
  }

  /**
   * ------------------------------------------------------------------------
   * Update Queries
   * ------------------------------------------------------------------------
   */
  /**
   * Add a Person to the social network. [1]
   */
  public static class LdbcUpdate1AddPersonHandler implements
      OperationHandler<LdbcUpdate1AddPerson, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcUpdate1AddPersonHandler.class);

    private final Calendar calendar;

    public LdbcUpdate1AddPersonHandler() {
      this.calendar = new GregorianCalendar();
    }

    @Override
    public void executeOperation(LdbcUpdate1AddPerson operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {
      Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

      // Build key value properties array
      List<Object> personKeyValues =
          new ArrayList<>(18 + 2 * operation.languages().size()
              + 2 * operation.emails().size());
      personKeyValues.add(T.id);
      personKeyValues.add(
          new UInt128(TorcEntity.PERSON.idSpace, operation.personId()));
      personKeyValues.add(T.label);
      personKeyValues.add(TorcEntity.PERSON.label);
      personKeyValues.add("firstName");
      personKeyValues.add(operation.personFirstName());
      personKeyValues.add("lastName");
      personKeyValues.add(operation.personLastName());
      personKeyValues.add("gender");
      personKeyValues.add(operation.gender());
      personKeyValues.add("birthday");
      personKeyValues.add(String.valueOf(operation.birthday().getTime()));
      personKeyValues.add("creationDate");
      personKeyValues.add(String.valueOf(operation.creationDate().getTime()));
      personKeyValues.add("locationIP");
      personKeyValues.add(operation.locationIp());
      personKeyValues.add("browserUsed");
      personKeyValues.add(operation.browserUsed());

      for (String language : operation.languages()) {
        personKeyValues.add("language");
        personKeyValues.add(language);
      }

      for (String email : operation.emails()) {
        personKeyValues.add("email");
        personKeyValues.add(email);
      }

      boolean txSucceeded = false;
      int txFailCount = 0;
      do {
        // Add person
        Vertex person = client.addVertex(personKeyValues.toArray());

        // Add edge to place
        Vertex place = client.vertices(
            new UInt128(TorcEntity.PLACE.idSpace, operation.cityId())).next();
        person.addEdge("isLocatedIn", place);

        // Add edges to tags
        List<UInt128> tagIds = new ArrayList<>(operation.tagIds().size());
        operation.tagIds().forEach((id) ->
            tagIds.add(new UInt128(TorcEntity.TAG.idSpace, id)));
        Iterator<Vertex> tagVItr = client.vertices(tagIds.toArray());
        tagVItr.forEachRemaining((tag) -> {
          person.addEdge("hasInterest", tag);
        });

        // Add edges to universities
        List<Object> studiedAtKeyValues = new ArrayList<>(2);
        for (LdbcUpdate1AddPerson.Organization org : operation.studyAt()) {
          studiedAtKeyValues.clear();
          studiedAtKeyValues.add("classYear");
          studiedAtKeyValues.add(String.valueOf(org.year()));
          Vertex orgV = client.vertices(
              new UInt128(TorcEntity.ORGANISATION.idSpace,
                  org.organizationId()))
              .next();
          person.addEdge("studyAt", orgV, studiedAtKeyValues.toArray());
        }

        // Add edges to companies
        List<Object> workedAtKeyValues = new ArrayList<>(2);
        for (LdbcUpdate1AddPerson.Organization org : operation.workAt()) {
          workedAtKeyValues.clear();
          workedAtKeyValues.add("workFrom");
          workedAtKeyValues.add(String.valueOf(org.year()));
          Vertex orgV = client.vertices(
              new UInt128(TorcEntity.ORGANISATION.idSpace,
                  org.organizationId())).next();
          person.addEdge("workAt", orgV, workedAtKeyValues.toArray());
        }

        try {
          client.tx().commit();
          txSucceeded = true;
        } catch (Exception e) {
          txFailCount++;
        }

        if (txFailCount >= MAX_TX_ATTEMPTS) {
          throw new RuntimeException(String.format(
              "ERROR: Transaction failed %d times, aborting...",
              txFailCount));
        }
      } while (!txSucceeded);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Like to a Post of the social network.[1]
   */
  public static class LdbcUpdate2AddPostLikeHandler implements
      OperationHandler<LdbcUpdate2AddPostLike, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcUpdate2AddPostLikeHandler.class);

    @Override
    public void executeOperation(LdbcUpdate2AddPostLike operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {
      Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

      UInt128 personId =
          new UInt128(TorcEntity.PERSON.idSpace, operation.personId());
      UInt128 postId =
          new UInt128(TorcEntity.POST.idSpace, operation.postId());

      boolean txSucceeded = false;
      int txFailCount = 0;
      do {
        Iterator<Vertex> results = client.vertices(personId, postId);
        Vertex person = results.next();
        Vertex post = results.next();
        List<Object> keyValues = new ArrayList<>(2);
        keyValues.add("creationDate");
        keyValues.add(String.valueOf(operation.creationDate().getTime()));
        person.addEdge("likes", post, keyValues.toArray());

        try {
          client.tx().commit();
          txSucceeded = true;
        } catch (Exception e) {
          txFailCount++;
        }

        if (txFailCount >= MAX_TX_ATTEMPTS) {
          throw new RuntimeException(String.format(
              "ERROR: Transaction failed %d times, aborting...",
              txFailCount));
        }
      } while (!txSucceeded);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Like to a Comment of the social network.[1]
   */
  public static class LdbcUpdate3AddCommentLikeHandler implements
      OperationHandler<LdbcUpdate3AddCommentLike, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcUpdate3AddCommentLikeHandler.class);

    @Override
    public void executeOperation(LdbcUpdate3AddCommentLike operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {
      Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

      UInt128 personId =
          new UInt128(TorcEntity.PERSON.idSpace, operation.personId());
      UInt128 commentId =
          new UInt128(TorcEntity.COMMENT.idSpace, operation.commentId());

      boolean txSucceeded = false;
      int txFailCount = 0;
      do {
        Iterator<Vertex> results = client.vertices(personId, commentId);
        Vertex person = results.next();
        Vertex comment = results.next();
        List<Object> keyValues = new ArrayList<>(2);
        keyValues.add("creationDate");
        keyValues.add(String.valueOf(operation.creationDate().getTime()));
        person.addEdge("likes", comment, keyValues.toArray());

        try {
          client.tx().commit();
          txSucceeded = true;
        } catch (Exception e) {
          txFailCount++;
        }

        if (txFailCount >= MAX_TX_ATTEMPTS) {
          throw new RuntimeException(String.format(
              "ERROR: Transaction failed %d times, aborting...",
              txFailCount));
        }
      } while (!txSucceeded);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Forum to the social network.[1]
   */
  public static class LdbcUpdate4AddForumHandler implements
      OperationHandler<LdbcUpdate4AddForum, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcUpdate4AddForum.class);

    @Override
    public void executeOperation(LdbcUpdate4AddForum operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {
      Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

      List<Object> forumKeyValues = new ArrayList<>(8);
      forumKeyValues.add(T.id);
      forumKeyValues.add(
          new UInt128(TorcEntity.FORUM.idSpace, operation.forumId()));
      forumKeyValues.add(T.label);
      forumKeyValues.add(TorcEntity.FORUM.label);
      forumKeyValues.add("title");
      forumKeyValues.add(operation.forumTitle());
      forumKeyValues.add("creationDate");
      forumKeyValues.add(String.valueOf(operation.creationDate().getTime()));

      boolean txSucceeded = false;
      int txFailCount = 0;
      do {
        Vertex forum = client.addVertex(forumKeyValues.toArray());

        List<UInt128> ids = new ArrayList<>(operation.tagIds().size() + 1);
        operation.tagIds().forEach((id) -> {
          ids.add(new UInt128(TorcEntity.TAG.idSpace, id));
        });
        ids.add(new UInt128(TorcEntity.PERSON.idSpace,
            operation.moderatorPersonId()));

        client.vertices(ids.toArray()).forEachRemaining((v) -> {
          if (v.label().equals(TorcEntity.TAG.label)) {
            forum.addEdge("hasTag", v);
          } else if (v.label().equals(TorcEntity.PERSON.label)) {
            forum.addEdge("hasModerator", v);
          } else {
            throw new RuntimeException(String.format(
                "ERROR: LdbcUpdate4AddForum query read a vertex with an "
                + "unexpected label: %s", v.label()));
          }
        });

        try {
          client.tx().commit();
          txSucceeded = true;
        } catch (Exception e) {
          txFailCount++;
        }

        if (txFailCount >= MAX_TX_ATTEMPTS) {
          throw new RuntimeException(String.format(
              "ERROR: Transaction failed %d times, aborting...",
              txFailCount));
        }
      } while (!txSucceeded);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Forum membership to the social network.[1]
   */
  public static class LdbcUpdate5AddForumMembershipHandler implements
      OperationHandler<LdbcUpdate5AddForumMembership, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcUpdate4AddForum.class);

    @Override
    public void executeOperation(LdbcUpdate5AddForumMembership operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {
      Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

      List<UInt128> ids = new ArrayList<>(2);
      ids.add(new UInt128(TorcEntity.FORUM.idSpace, operation.forumId()));
      ids.add(new UInt128(TorcEntity.PERSON.idSpace, operation.personId()));

      boolean txSucceeded = false;
      int txFailCount = 0;
      do {
        Iterator<Vertex> vItr = client.vertices(ids.toArray());
        Vertex forum = vItr.next();
        Vertex member = vItr.next();

        List<Object> edgeKeyValues = new ArrayList<>(2);
        edgeKeyValues.add("joinDate");
        edgeKeyValues.add(String.valueOf(operation.joinDate().getTime()));

        forum.addEdge("hasMember", member, edgeKeyValues.toArray());

        try {
          client.tx().commit();
          txSucceeded = true;
        } catch (Exception e) {
          txFailCount++;
        }

        if (txFailCount >= MAX_TX_ATTEMPTS) {
          throw new RuntimeException(String.format(
              "ERROR: Transaction failed %d times, aborting...",
              txFailCount));
        }
      } while (!txSucceeded);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Post to the social network.[1]
   */
  public static class LdbcUpdate6AddPostHandler implements
      OperationHandler<LdbcUpdate6AddPost, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcUpdate4AddForum.class);

    @Override
    public void executeOperation(LdbcUpdate6AddPost operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {
      Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

      List<Object> postKeyValues = new ArrayList<>(18);
      postKeyValues.add(T.id);
      postKeyValues.add(
          new UInt128(TorcEntity.POST.idSpace, operation.postId()));
      postKeyValues.add(T.label);
      postKeyValues.add(TorcEntity.POST.label);
      postKeyValues.add("imageFile");
      postKeyValues.add(operation.imageFile());
      postKeyValues.add("creationDate");
      postKeyValues.add(String.valueOf(operation.creationDate().getTime()));
      postKeyValues.add("locationIP");
      postKeyValues.add(operation.locationIp());
      postKeyValues.add("browserUsed");
      postKeyValues.add(operation.browserUsed());
      postKeyValues.add("language");
      postKeyValues.add(operation.language());
      postKeyValues.add("content");
      postKeyValues.add(operation.content());
      postKeyValues.add("length");
      postKeyValues.add(String.valueOf(operation.length()));

      boolean txSucceeded = false;
      int txFailCount = 0;
      do {
        Vertex post = client.addVertex(postKeyValues.toArray());

        List<UInt128> ids = new ArrayList<>(2);
        ids.add(new UInt128(TorcEntity.PERSON.idSpace,
            operation.authorPersonId()));
        ids.add(new UInt128(TorcEntity.FORUM.idSpace, operation.forumId()));
        ids.add(new UInt128(TorcEntity.PLACE.idSpace, operation.countryId()));
        operation.tagIds().forEach((id) -> {
          ids.add(new UInt128(TorcEntity.TAG.idSpace, id));
        });

        client.vertices(ids.toArray()).forEachRemaining((v) -> {
          if (v.label().equals(TorcEntity.PERSON.label)) {
            post.addEdge("hasCreator", v);
          } else if (v.label().equals(TorcEntity.FORUM.label)) {
            v.addEdge("containerOf", post);
          } else if (v.label().equals(TorcEntity.PLACE.label)) {
            post.addEdge("isLocatedIn", v);
          } else if (v.label().equals(TorcEntity.TAG.label)) {
            post.addEdge("hasTag", v);
          } else {
            throw new RuntimeException(String.format(
                "ERROR: LdbcUpdate6AddPostHandler query read a vertex with an "
                + "unexpected label: %s", v.label()));
          }
        });

        try {
          client.tx().commit();
          txSucceeded = true;
        } catch (Exception e) {
          txFailCount++;
        }

        if (txFailCount >= MAX_TX_ATTEMPTS) {
          throw new RuntimeException(String.format(
              "ERROR: Transaction failed %d times, aborting...",
              txFailCount));
        }
      } while (!txSucceeded);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Comment replying to a Post/Comment to the social network.[1]
   */
  public static class LdbcUpdate7AddCommentHandler implements
      OperationHandler<LdbcUpdate7AddComment, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcUpdate4AddForum.class);

    @Override
    public void executeOperation(LdbcUpdate7AddComment operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {
      Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

      List<Object> commentKeyValues = new ArrayList<>(14);
      commentKeyValues.add(T.id);
      commentKeyValues.add(
          new UInt128(TorcEntity.COMMENT.idSpace, operation.commentId()));
      commentKeyValues.add(T.label);
      commentKeyValues.add(TorcEntity.COMMENT.label);
      commentKeyValues.add("creationDate");
      commentKeyValues.add(String.valueOf(operation.creationDate().getTime()));
      commentKeyValues.add("locationIP");
      commentKeyValues.add(operation.locationIp());
      commentKeyValues.add("browserUsed");
      commentKeyValues.add(operation.browserUsed());
      commentKeyValues.add("content");
      commentKeyValues.add(operation.content());
      commentKeyValues.add("length");
      commentKeyValues.add(String.valueOf(operation.length()));

      boolean txSucceeded = false;
      int txFailCount = 0;
      do {
        Vertex comment = client.addVertex(commentKeyValues.toArray());

        List<UInt128> ids = new ArrayList<>(2);
        ids.add(new UInt128(TorcEntity.PERSON.idSpace,
            operation.authorPersonId()));
        ids.add(new UInt128(TorcEntity.PLACE.idSpace, operation.countryId()));
        operation.tagIds().forEach((id) -> {
          ids.add(new UInt128(TorcEntity.TAG.idSpace, id));
        });
        if (operation.replyToCommentId() != -1) {
          ids.add(new UInt128(TorcEntity.COMMENT.idSpace,
              operation.replyToCommentId()));
        }
        if (operation.replyToPostId() != -1) {
          ids.add(
              new UInt128(TorcEntity.POST.idSpace, operation.replyToPostId()));
        }

        client.vertices(ids.toArray()).forEachRemaining((v) -> {
          if (v.label().equals(TorcEntity.PERSON.label)) {
            comment.addEdge("hasCreator", v);
          } else if (v.label().equals(TorcEntity.PLACE.label)) {
            comment.addEdge("isLocatedIn", v);
          } else if (v.label().equals(TorcEntity.COMMENT.label)) {
            comment.addEdge("replyOf", v);
          } else if (v.label().equals(TorcEntity.POST.label)) {
            comment.addEdge("replyOf", v);
          } else if (v.label().equals(TorcEntity.TAG.label)) {
            comment.addEdge("hasTag", v);
          } else {
            throw new RuntimeException(String.format(
                "ERROR: LdbcUpdate7AddCommentHandler query read a vertex with "
                + "an unexpected label: %s", v.label()));
          }
        });

        try {
          client.tx().commit();
          txSucceeded = true;
        } catch (Exception e) {
          txFailCount++;
        }

        if (txFailCount >= MAX_TX_ATTEMPTS) {
          throw new RuntimeException(String.format(
              "ERROR: Transaction failed %d times, aborting...",
              txFailCount));
        }
      } while (!txSucceeded);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a friendship relation to the social network.[1]
   */
  public static class LdbcUpdate8AddFriendshipHandler implements
      OperationHandler<LdbcUpdate8AddFriendship, DbConnectionState> {

    final static Logger logger =
        LoggerFactory.getLogger(LdbcUpdate4AddForum.class);

    @Override
    public void executeOperation(LdbcUpdate8AddFriendship operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {
      Graph client = ((TorcDbConnectionState) dbConnectionState).getClient();

      List<Object> knowsEdgeKeyValues = new ArrayList<>(2);
      knowsEdgeKeyValues.add("creationDate");
      knowsEdgeKeyValues.add(
          String.valueOf(operation.creationDate().getTime()));

      List<UInt128> ids = new ArrayList<>(2);
      ids.add(new UInt128(TorcEntity.PERSON.idSpace, operation.person1Id()));
      ids.add(new UInt128(TorcEntity.PERSON.idSpace, operation.person2Id()));

      boolean txSucceeded = false;
      int txFailCount = 0;
      do {
        Iterator<Vertex> vItr = client.vertices(ids.toArray());

        Vertex person1 = vItr.next();
        Vertex person2 = vItr.next();

        person1.addEdge("knows", person2, knowsEdgeKeyValues.toArray());
        person2.addEdge("knows", person1, knowsEdgeKeyValues.toArray());

        try {
          client.tx().commit();
          txSucceeded = true;
        } catch (Exception e) {
          txFailCount++;
        }

        if (txFailCount >= MAX_TX_ATTEMPTS) {
          throw new RuntimeException(String.format(
              "ERROR: Transaction failed %d times, aborting...",
              txFailCount));
        }
      } while (!txSucceeded);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }
}
