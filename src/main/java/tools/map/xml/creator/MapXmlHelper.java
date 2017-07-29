package tools.map.xml.creator;

import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_ALLIANCE;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_ATTACHMENT;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_ATTACHMENT_LIST;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_BOOLEAN;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_CONNECTION;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_COST;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_DELEGATE;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_FRONTIER_RULES;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_GAME;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_GAME_PLAY;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_INITIALIZE;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_MAP;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_NUMBER;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_OPTION;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_OWNER_INITIALIZE;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_PLAYER;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_PLAYER_LIST;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_PRODUCTION;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_PRODUCTION_FRONTIER;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_PRODUCTION_RULE;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_PROPERTY;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_PROPERTY_LIST;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_RESOURCE;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_RESOURCE_LIST;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_RESULT;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_SEQUENCE;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_STEP;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_TERRITORY;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_TERRITORY_OWNER;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_UNIT;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_UNIT_INITIALIZE;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_UNIT_LIST;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_UNIT_PLACEMENT;
import static games.strategy.engine.data.TripleASaxHandler.XML_NODE_NAME_VALUE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.TripleASaxHandler;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.CanalAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.util.Triple;
import tools.map.xml.creator.MapXmlCreator.GameStep;
import tools.map.xml.creator.TerritoryDefinitionDialog.Definition;

/**
 * This class reads, writes and keeps the Map XML properties.
 */
public class MapXmlHelper {

  private static final String XML_ATTR_VALUE_SEPARATOR_OPTION_VALUE = ":";
  public static final String XML_ATTR_STEP_NAME_DISPLAY = "display";
  public static final String XML_ATTR_CONNECTION_NAME_T2 = "t2";
  public static final String XML_ATTR_CONNECTION_NAME_T1 = "t1";

  // XML Attribute Constants
  public static final String XML_ATTR_ALLIANCE_NAME_PLAYER = "player";
  public static final String XML_ATTR_ALLIANCE_NAME_ALLIANCE = "alliance";

  public static final String XML_ATTR_ATTACHMENT_NAME_NAME = "name";
  public static final String XML_ATTR_ATTACHMENT_NAME_ATTACH_TO = "attachTo";
  public static final String XML_ATTR_ATTACHMENT_NAME_TYPE = "type";
  public static final String XML_ATTR_ATTACHMENT_NAME_JAVA_CLASS = "javaClass";
  public static final String XML_ATTR_ATTACHMENT_NAME_TERRITORY = "territory";
  public static final String XML_ATTR_ATTACHMENT_NAME_UNIT_TYPE = "unitType";
  public static final String XML_ATTR_ATTACHMENT_NAME_PLAYER = "player";

  public static final String XML_ATTR_COST_NAME_RESOURCE = "resource";

  public static final String XML_ATTR_NUMBER_NAME_MAX = "max";
  public static final String XML_ATTR_NUMBER_NAME_MIN = "min";

  public static final String XML_ATTR_OPTION_NAME_NAME = "name";
  public static final String XML_ATTR_OPTION_NAME_VALUE = "value";

  public static final String XML_ATTR_PLAYER_NAME_NAME = "name";
  public static final String XML_ATTR_PLAYER_NAME_OPTIONAL = "optional";

  public static final String XML_ATTR_PROPERTY_NAME_NAME = "name";
  public static final String XML_ATTR_PROPERTY_NAME_VALUE = "value";
  public static final String XML_ATTR_PROPERTY_NAME_EDITABLE = "editable";

  public static final String XML_ATTR_RESOURCE_NAME_NAME = "name";

  public static final String XML_ATTR_RESULT_NAME_QUANTITY = "quantity";
  public static final String XML_ATTR_RESULT_NAME_RESOURCE_OR_UNIT = "resourceOrUnit";

  public static final String XML_ATTR_STEP_NAME_DELEGATE = "delegate";
  public static final String XML_ATTR_STEP_NAME_PLAYER = "player";
  public static final String XML_ATTR_STEP_NAME_NAME = "name";
  public static final String XML_ATTR_STEP_NAME_MAX_RUN_COUNT = "maxRunCount";

  public static final String XML_ATTR_TERRITORY_OWNER_NAME_TERRITORY = "territoryOwner";

  public static final String XML_ATTR_UNIT_PLACEMENT_NAME_OWNER = "owner";
  public static final String XML_ATTR_UNIT_PLACEMENT_NAME_TERRITORY = "territory";
  public static final String XML_ATTR_UNIT_PLACEMENT_NAME_UNIT_TYPE = "unitType";

  // XML Attribute Value Constants
  public static final String XML_ATTR_VALUE_PLAYER_OPTIONAL_NAME_FALSE = "false";
  public static final String XML_ATTR_VALUE_OPTION_NAME_CANAL_NAME = "canalName";
  public static final String XML_ATTR_VALUE_OPTION_NAME_LAND_TERRITORIES = "landTerritories";
  public static final String XML_ATTR_VALUE_PROPERTY_NAME_MAP_NAME = "mapName";
  public static final String XML_ATTR_VALUE_PROPERTY_NAME_NOTES = "notes";


  public static final String TRIPLEA_JAVA_CLASS_DELEGATE_PATH =
      BattleDelegate.class.getPackage().toString().concat(".");

  ///////////////////////////////////////////
  // Getter and Setter methods for mapXmlData attributes
  ///////////////////////////////////////////
  public static Map<String, String> getXmlStringsMap() {
    return mapXmlData.getXmlStringsMap();
  }

  public static void setXmlStrings(final Map<String, String> xmlStrings) {
    mapXmlData.setXmlStringsMap(xmlStrings);
  }

  public static List<String> getResourceList() {
    return mapXmlData.getResourceList();
  }

  public static void setResourceList(final List<String> resourceList) {
    mapXmlData.setResourceList(resourceList);
  }

  public static Map<String, Map<Definition, Boolean>> getTerritoryDefintionsMap() {
    return mapXmlData.getTerritoryDefintionsMap();
  }

  public static void setTerritoryDefintions(final Map<String, Map<Definition, Boolean>> territoryDefintions) {
    mapXmlData.setTerritoryDefintionsMap(territoryDefintions);
  }

  public static Map<String, Set<String>> getTerritoryConnectionsMap() {
    return mapXmlData.getTerritoryConnectionsMap();
  }

  public static void setTerritoryConnections(final Map<String, Set<String>> territoryConnections) {
    mapXmlData.setTerritoryConnectionsMap(territoryConnections);
  }

  public static List<String> getPlayerNames() {
    return mapXmlData.getPlayerNames();
  }

  public static void setPlayerNames(final List<String> playerName) {
    mapXmlData.setPlayerNames(playerName);
  }

  public static Map<String, String> getPlayerAllianceMap() {
    return mapXmlData.getPlayerAllianceMap();
  }

  public static void setPlayerAlliance(final Map<String, String> playerAlliance) {
    mapXmlData.setPlayerAllianceMap(playerAlliance);
  }

  public static Map<String, Integer> getPlayerInitResourcesMap() {
    return mapXmlData.getPlayerInitResourcesMap();
  }

  public static void setPlayerInitResources(final Map<String, Integer> playerInitResources) {
    mapXmlData.setPlayerInitResourcesMap(playerInitResources);
  }

  public static Map<String, List<Integer>> getUnitDefinitionsMap() {
    return mapXmlData.getUnitDefinitionsMap();
  }

  public static void setUnitDefinitions(final Map<String, List<Integer>> unitDefinitions) {
    mapXmlData.setUnitDefinitionsMap(unitDefinitions);
  }

  public static Map<String, List<String>> getGamePlaySequenceMap() {
    return mapXmlData.getGamePlaySequenceMap();
  }

  public static void setGamePlaySequence(final Map<String, List<String>> gamePlaySequence) {
    mapXmlData.setGamePlaySequenceMap(gamePlaySequence);
  }

  public static Map<String, Triple<String, String, Integer>> getPlayerSequenceMap() {
    return mapXmlData.getPlayerSequenceMap();
  }

  public static void setPlayerSequence(final Map<String, Triple<String, String, Integer>> playerSequence) {
    mapXmlData.setPlayerSequenceMap(playerSequence);
  }

  public static Map<String, List<String>> getTechnologyDefinitionsMap() {
    return mapXmlData.getTechnologyDefinitionsMap();
  }

  public static void setTechnologyDefinitions(final Map<String, List<String>> technologyDefinitions) {
    mapXmlData.setTechnologyDefinitionsMap(technologyDefinitions);
  }

  public static Map<String, List<String>> getProductionFrontiersMap() {
    return mapXmlData.getProductionFrontiersMap();
  }

  public static void setProductionFrontiers(final Map<String, List<String>> productionFrontiers) {
    mapXmlData.setProductionFrontiersMap(productionFrontiers);
  }

  public static Map<String, List<String>> getUnitAttachmentsMap() {
    return mapXmlData.getUnitAttachmentsMap();
  }

  public static void setUnitAttachments(final Map<String, List<String>> unitAttachments) {
    mapXmlData.setUnitAttachmentsMap(unitAttachments);
  }

  public static Map<String, Integer> getTerritoyProductionsMap() {
    return mapXmlData.getTerritoyProductionsMap();
  }

  public static void setTerritoyProductions(final Map<String, Integer> territoyProductions) {
    mapXmlData.setTerritoyProductionsMap(territoyProductions);
  }

  public static Map<String, CanalTerritoriesTuple> getCanalDefinitionsMap() {
    return mapXmlData.getCanalDefinitionsMap();
  }

  public static void setCanalDefinitions(final Map<String, CanalTerritoriesTuple> canalDefinitions) {
    mapXmlData.setCanalDefinitionsMap(canalDefinitions);
  }

  public static Map<String, String> getTerritoryOwnershipsMap() {
    return mapXmlData.getTerritoryOwnershipsMap();
  }

  public static void setTerritoryOwnerships(final Map<String, String> territoryOwnerships) {
    mapXmlData.setTerritoryOwnershipsMap(territoryOwnerships);
  }

  public static Map<String, Map<String, Map<String, Integer>>> getUnitPlacementsMap() {
    return mapXmlData.getUnitPlacementsMap();
  }

  public static void setUnitPlacements(final Map<String, Map<String, Map<String, Integer>>> unitPlacements) {
    mapXmlData.setUnitPlacementsMap(unitPlacements);
  }

  public static Map<String, List<String>> getGameSettingsMap() {
    return mapXmlData.getGameSettingsMap();
  }

  public static void setGameSettings(final Map<String, List<String>> gameSettings) {
    mapXmlData.setGameSettingsMap(gameSettings);
  }

  public static String getNotes() {
    return mapXmlData.getNotes();
  }

  public static void setNotes(final String notes) {
    mapXmlData.setNotes(notes);
  }

  public static File getMapXmlFile() {
    return mapXmlData.getMapXmlFile();
  }

  public static void setMapXmlFile(final File mapXmlFile) {
    MapXmlHelper.mapXmlData.setMapXmlFile(mapXmlFile);
  }

  static void setMapXmlData(final MapXmlData mapXmlData) {
    MapXmlHelper.mapXmlData = mapXmlData;
  }

  private static MapXmlData mapXmlData = new MapXmlData();

  static void putXmlStrings(final String key, final String value) {
    getXmlStringsMap().put(key, value);
  }

  static void addResourceList(final String value) {
    getResourceList().add(value);
  }

  static void addResourceList(final int index, final String value) {
    getResourceList().add(index, value);
  }

  static void putTerritoryDefintions(final String key, final Map<Definition, Boolean> value) {
    getTerritoryDefintionsMap().put(key, value);
  }

  static void putTerritoryConnections(final String key, final Set<String> value) {
    getTerritoryConnectionsMap().put(key, value);
  }

  static void addPlayerName(final String value) {
    getPlayerNames().add(value);
  }

  static void putPlayerAlliance(final String key, final String value) {
    getPlayerAllianceMap().put(key, value);
  }

  static void putPlayerInitResources(final String key, final int value) {
    getPlayerInitResourcesMap().put(key, value);
  }

  static void putUnitDefinitions(final String key, final List<Integer> value) {
    getUnitDefinitionsMap().put(key, value);
  }

  static void putGamePlaySequence(final String key, final List<String> value) {
    getGamePlaySequenceMap().put(key, value);
  }

  static void putPlayerSequence(final String key, final Triple<String, String, Integer> value) {
    getPlayerSequenceMap().put(key, value);
  }

  static void putTechnologyDefinitions(final String key, final List<String> value) {
    getTechnologyDefinitionsMap().put(key, value);
  }

  static void putProductionFrontiers(final String key, final List<String> value) {
    getProductionFrontiersMap().put(key, value);
  }

  static void putUnitAttachments(final String key, final List<String> value) {
    getUnitAttachmentsMap().put(key, value);
  }

  static void putTerritoyProductions(final String key, final int value) {
    getTerritoyProductionsMap().put(key, value);
  }

  static void putCanalDefinitions(final String key, final CanalTerritoriesTuple value) {
    getCanalDefinitionsMap().put(key, value);
  }

  static void putTerritoyOwnerships(final String key, final String value) {
    getTerritoryOwnershipsMap().put(key, value);
  }

  static void putUnitPlacements(final String key, final Map<String, Map<String, Integer>> value) {
    getUnitPlacementsMap().put(key, value);
  }

  static void putGameSettings(final String key, final List<String> value) {
    getGameSettingsMap().put(key, value);
  }

  static void clearXmlStrings() {
    getXmlStringsMap().clear();
  }

  static void clearResourceList() {
    getResourceList().clear();
  }

  static void clearTerritoyDefintions() {
    getTerritoryDefintionsMap().clear();
  }

  static void clearTerritoryConnections() {
    getTerritoryConnectionsMap().clear();
  }

  static void clearPlayerName() {
    getPlayerNames().clear();
  }

  static void clearPlayerAlliance() {
    getPlayerAllianceMap().clear();
  }

  static void clearPlayerInitResources() {
    getPlayerInitResourcesMap().clear();
  }

  static void clearUnitDefinitions() {
    getUnitDefinitionsMap().clear();
  }

  static void clearGamePlaySequence() {
    getGamePlaySequenceMap().clear();
  }

  static void clearPlayerSequence() {
    getPlayerSequenceMap().clear();
  }

  static void clearTechnologyDefinitions() {
    getTechnologyDefinitionsMap().clear();
  }

  static void clearProductionFrontiers() {
    getProductionFrontiersMap().clear();
  }

  static void clearUnitAttachments() {
    getUnitAttachmentsMap().clear();
  }

  static void clearTerritoyProductions() {
    getTerritoyProductionsMap().clear();
  }

  static void clearCanalDefinitions() {
    getCanalDefinitionsMap().clear();
  }

  static void clearTerritoyOwnerships() {
    getTerritoryOwnershipsMap().clear();
  }

  static void clearUnitPlacements() {
    getUnitPlacementsMap().clear();
  }

  static void clearGameSettings() {
    getGameSettingsMap().clear();
  }

  ///////////////////////////////////////////
  // Start of XML parsing methods
  ///////////////////////////////////////////
  static GameStep parseValuesFromXml(final String gameXmlPath)
      throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
    initializeAll();
    Wrapper<GameStep> stepToGo = new Wrapper<>(MapXmlCreator.GAME_STEP_FIRST);

    new GameParser(gameXmlPath).parse(new FileInputStream(gameXmlPath), new TripleASaxHandler(null) {
      private String costQuantity = null;
      private String resultQuantity = null;
      private String resourceOrUnit = null;

      private String playerName = null;
      private final List<String> frontierRules = new ArrayList<>();


      private CanalTerritoriesTuple canalDef = null;
      private String newCanalName = null;
      private SortedSet<String> newLandTerritories = new TreeSet<>();
      private String attachmentAttachTo = null;

      private List<String> settingValues = new ArrayList<>();

      @Override
      protected void handleInfo(Attributes attributes) {
        for (int i = 0; i < attributes.getLength(); i++) {
          getXmlStringsMap().put("info_@" + attributes.getQName(i), attributes.getValue(i));
        }
      }

      @Override
      protected void handleLoader(Attributes attributes) {}

      @Override
      protected void handleTriplea(Attributes attributes) {}

      @Override
      protected void handleDiceSides(Attributes attributes) {}

      @Override
      protected void handleTerritory(Attributes attributes) {
        Map<Definition, Boolean> terrDef = new HashMap<>();
        String terrName = null;
        for (int i = 0; i < attributes.getLength(); i++) {
          if (attributes.getQName(i).equals(XML_ATTR_ATTACHMENT_NAME_NAME)) {
            terrName = attributes.getValue(i);
          } else {
            terrDef.put(TerritoryDefinitionDialog.valueOf(attributes.getQName(i)),
                Boolean.valueOf(attributes.getValue(i)));
          }
        }
        getTerritoryDefintionsMap().put(terrName, terrDef);
      }

      @Override
      protected void handleConnection(Attributes attributes) {
        String t1Name = attributes.getValue(0);
        String t2Name = attributes.getValue(1);
        if (t1Name.compareTo(t2Name) > 0) {
          final String swapHelper = t1Name;
          t1Name = t2Name;
          t2Name = swapHelper;
        }
        Set<String> t1Connections = getTerritoryConnectionsMap().get(t1Name);
        if (t1Connections != null) {
          t1Connections.add(t2Name);
        } else {
          t1Connections = Sets.newLinkedHashSet();
          t1Connections.add(t2Name);
          getTerritoryConnectionsMap().put(t1Name, t1Connections);
        }
      }

      @Override
      protected void handleResource(Attributes attributes) {
        getResourceList().add(attributes.getValue(0));
      }

      @Override
      protected void handlePlayer(Attributes attributes) {
        final String playerNameAttr = attributes.getValue(XML_ATTR_PLAYER_NAME_NAME);
        getPlayerNames().add(playerNameAttr);
        getPlayerInitResourcesMap().put(playerNameAttr, 0);
        // TODO: add logic for optional value
        // attrMapPlayer.get("optional")
      }

      @Override
      protected void handleAlliance(Attributes attributes) {
        getPlayerAllianceMap().put(attributes.getValue(XML_ATTR_ALLIANCE_NAME_PLAYER),
            attributes.getValue(XML_ATTR_ALLIANCE_NAME_ALLIANCE));
      }

      @Override
      protected void handleUnit(Attributes attributes) {}

      @Override
      protected void handleDelegate(Attributes attributes) {
        getGamePlaySequenceMap().put(attributes.getValue(XML_ATTR_ATTACHMENT_NAME_NAME),
            Arrays.asList(
                attributes.getValue(XML_ATTR_ATTACHMENT_NAME_JAVA_CLASS).replace(TRIPLEA_JAVA_CLASS_DELEGATE_PATH, ""),
                attributes.getValue(XML_ATTR_STEP_NAME_DISPLAY)));
      }

      @Override
      protected void handleStep(Attributes attributes) {
        final String maxRunCount = attributes.getValue(XML_ATTR_STEP_NAME_MAX_RUN_COUNT);
        getPlayerSequenceMap().put(attributes.getValue(XML_ATTR_STEP_NAME_NAME),
            Triple.of(attributes.getValue(XML_ATTR_STEP_NAME_DELEGATE),
                Strings.nullToEmpty(attributes.getValue(XML_ATTR_STEP_NAME_PLAYER)),
                (maxRunCount == null ? 0 : Integer.parseInt(maxRunCount))));
      }

      @Override
      protected void handlePlayerProduction(Attributes attributes) {}

      @Override
      protected void handleCost(Attributes attributes) {
        costQuantity = attributes.getValue(XML_ATTR_RESULT_NAME_QUANTITY);
      }

      @Override
      protected void handleResult(Attributes attributes) {
        resultQuantity = attributes.getValue(XML_ATTR_RESULT_NAME_QUANTITY);
        resourceOrUnit = attributes.getValue(XML_ATTR_RESULT_NAME_RESOURCE_OR_UNIT);
      }

      @Override
      protected void handleFrontierRules(Attributes attributes) {
        frontierRules.add(attributes.getValue(XML_ATTR_ATTACHMENT_NAME_NAME).substring(3));
      }

      @Override
      protected void handleOption(Attributes attributes) {
        final Attributes parentAttributes = getCurrentParent().getAttributes();
        final String attachmentName = parentAttributes.getValue(XML_ATTR_ATTACHMENT_NAME_NAME);
        final String attachmentType = parentAttributes.getValue(XML_ATTR_ATTACHMENT_NAME_TYPE);
        final String attachmentAttachTo = parentAttributes.getValue(XML_ATTR_ATTACHMENT_NAME_ATTACH_TO);
        if (attachmentName.equals(Constants.TECH_ATTACHMENT_NAME)
            && attachmentType.equals(XML_ATTR_ATTACHMENT_NAME_PLAYER)) {
          getTechnologyDefinitionsMap().put(
              attributes.getValue(XML_ATTR_ATTACHMENT_NAME_NAME) + "_" + attachmentAttachTo,
              Arrays.asList(attachmentAttachTo, attributes.getValue(XML_NODE_NAME_VALUE)));
        } else if (attachmentName.equals(Constants.UNIT_ATTACHMENT_NAME)
            && attachmentType.equals(XML_ATTR_ATTACHMENT_NAME_UNIT_TYPE)) {
          getUnitAttachmentsMap().put(
              attributes.getValue(XML_ATTR_ATTACHMENT_NAME_NAME) + "_" + attachmentAttachTo,
              Arrays.asList(attachmentAttachTo, attributes.getValue(XML_NODE_NAME_VALUE)));
        } else if (attachmentName.equals(Constants.INF_ATTACHMENT_NAME)
            && attachmentType.equals(XML_ATTR_ATTACHMENT_NAME_TERRITORY) && canalDef == null) {
          final String attachOptAttrName = attributes.getValue(XML_ATTR_OPTION_NAME_NAME);
          if (attachOptAttrName.equals(XML_ATTR_VALUE_OPTION_NAME_CANAL_NAME)) {
            newCanalName = attributes.getValue(XML_NODE_NAME_VALUE);
            canalDef = getCanalDefinitionsMap().get(newCanalName);
            if (canalDef != null) {
              return;
            }
          } else if (attachOptAttrName.equals(XML_ATTR_VALUE_OPTION_NAME_LAND_TERRITORIES)) {
            newLandTerritories.addAll(Arrays
                .asList(attributes.getValue(XML_ATTR_OPTION_NAME_VALUE).split(XML_ATTR_VALUE_SEPARATOR_OPTION_VALUE)));
          }
        } else if (attachmentName.equals(Constants.TERRITORY_ATTACHMENT_NAME)
            && attachmentType.equals(XML_ATTR_ATTACHMENT_NAME_NAME)) {
          final String optionNameAttr = attributes.getValue(XML_ATTR_OPTION_NAME_NAME);
          if (optionNameAttr.equals(XML_NODE_NAME_PRODUCTION)) {
            getTerritoyProductionsMap().put(attachmentAttachTo,
                Integer.parseInt(attributes.getValue(XML_NODE_NAME_VALUE)));
          } else {
            final Map<Definition, Boolean> terrDefinitions =
                getTerritoryDefintionsMap().getOrDefault(attachmentAttachTo, new HashMap<>());
            terrDefinitions.put(TerritoryDefinitionDialog.valueOf(optionNameAttr), true);
            getTerritoryDefintionsMap().put(attachmentAttachTo, terrDefinitions);
          }
        }

      }

      @Override
      protected void handleUnitPlacement(Attributes attributes) {
        final String territory = attributes.getValue(XML_NODE_NAME_TERRITORY);
        final String owner = attributes.getValue(XML_ATTR_UNIT_PLACEMENT_NAME_OWNER);
        final Map<String, Map<String, Integer>> terrPlacements =
            getUnitPlacementsMap().getOrDefault(territory, new HashMap<>());
        final Map<String, Integer> terrOwnerPlacements = terrPlacements.getOrDefault(owner, new LinkedHashMap<>());
        terrOwnerPlacements.put(attributes.getValue(XML_ATTR_UNIT_PLACEMENT_NAME_UNIT_TYPE),
            Integer.parseInt(attributes.getValue(XML_ATTR_RESULT_NAME_QUANTITY)));
        terrPlacements.put(owner, terrOwnerPlacements);
        getUnitPlacementsMap().put(territory, terrPlacements);
      }

      @Override
      protected void handleTerritoryOwner(Attributes attributes) {
        getTerritoryOwnershipsMap().put(attributes.getValue(XML_NODE_NAME_TERRITORY),
            attributes.getValue(XML_ATTR_UNIT_PLACEMENT_NAME_OWNER));
      }

      @Override
      protected void handleResourceGiven(Attributes attributes) {}

      @Override
      protected void handleProperty(Attributes attributes) {
        settingValues = new ArrayList<>();
        final String propertyName = attributes.getValue(XML_ATTR_PROPERTY_NAME_NAME);
        if (propertyName.equals(XML_ATTR_VALUE_PROPERTY_NAME_NOTES)
            || propertyName.equals(XML_ATTR_VALUE_PROPERTY_NAME_MAP_NAME)) {
          setNotes(attributes.getValue(XML_NODE_NAME_VALUE));
          return;
        }
        settingValues.add(attributes.getValue(XML_NODE_NAME_VALUE));
        settingValues.add(Boolean.toString(Boolean.parseBoolean(attributes.getValue(XML_ATTR_PROPERTY_NAME_EDITABLE))));
      }

      @Override
      protected void handleNumber() {
        Attributes attributes = getCurrentParent().getAttributes();
        settingValues.add(attributes.getValue(XML_ATTR_NUMBER_NAME_MIN));
        settingValues.add(attributes.getValue(XML_ATTR_NUMBER_NAME_MAX));
        getGameSettingsMap().put(attributes.getValue(XML_ATTR_PROPERTY_NAME_NAME), settingValues);
      }

      @Override
      protected void handleBoolean() {
        settingValues.add("0"); // min
        settingValues.add("0"); // max
        getGameSettingsMap().put(getCurrentParent().getAttributes().getValue(XML_ATTR_PROPERTY_NAME_NAME),
            settingValues);
      }

      @Override
      protected void handleTech(Attributes attributes) {}

      @Override
      protected void handleMap() {
        stepToGo.set(MapXmlCreator.getMaxGameStep(stepToGo.get(), (getTerritoryConnectionsMap().isEmpty()
            ? GameStep.TERRITORY_DEFINITIONS
            : GameStep.TERRITORY_CONNECTIONS)));
      }

      @Override
      protected void handlePlayerList() {
        stepToGo.set(MapXmlCreator.getMaxGameStep(stepToGo.get(), GameStep.PLAYERS_AND_ALLIANCES));
      }

      @Override
      protected void handleProduction() {
        stepToGo.set(MapXmlCreator.getMaxGameStep(stepToGo.get(),
            (getProductionFrontiersMap().isEmpty() ? GameStep.UNIT_DEFINITIONS : GameStep.PRODUCTION_FRONTIERS)));
      }

      @Override
      protected void handleProductionRule() {
        putUnitDefinitions(resourceOrUnit,
            Arrays.asList(Integer.parseInt(costQuantity), Integer.parseInt(resultQuantity)));
        resourceOrUnit = costQuantity = resultQuantity = null;
      }

      @Override
      protected void handleProductionFrontier(Attributes attributes) {
        String value = attributes.getValue(XML_ATTR_ATTACHMENT_NAME_NAME);
        playerName = value.length() >= 10 ? attributes.getValue(XML_ATTR_ATTACHMENT_NAME_NAME).substring(10) : "";
      }

      @Override
      protected void handleProductionFrontier() {
        getProductionFrontiersMap().put(playerName, frontierRules);
        frontierRules.clear();
        playerName = null;
      }

      @Override
      protected void handleGamePlay() {
        stepToGo.set(MapXmlCreator.getMaxGameStep(stepToGo.get(),
            getPlayerSequenceMap().isEmpty() ? GameStep.UNIT_ATTACHMENTS : GameStep.TERRITORY_PRODUCTION));
      }

      @Override
      protected void handleAttachmentList() {
        stepToGo.set(MapXmlCreator.getMaxGameStep(MapXmlCreator.getMaxGameStep(stepToGo.get(),
            getUnitAttachmentsMap().isEmpty() ? GameStep.PRODUCTION_FRONTIERS : GameStep.UNIT_ATTACHMENTS),
            getTerritoyProductionsMap().isEmpty() ? GameStep.UNIT_ATTACHMENTS : GameStep.TERRITORY_PRODUCTION));
      }

      @Override
      protected void handleAttachment(Attributes attributes) {
        attachmentAttachTo = attributes.getValue(XML_ATTR_ATTACHMENT_NAME_ATTACH_TO);
      }

      @Override
      protected void handleAttachment() {
        if (canalDef == null) {
          final SortedSet<String> newWaterTerritories = new TreeSet<>();
          newWaterTerritories.add(attachmentAttachTo);
          getCanalDefinitionsMap().put(newCanalName,
              new CanalTerritoriesTuple(newWaterTerritories, newLandTerritories));
        } else {
          canalDef.getWaterTerritories().add(attachmentAttachTo);
        }
        canalDef = null;
        newCanalName = null;
        newLandTerritories = new TreeSet<>();
      }

      @Override
      protected void handleInitialize() {
        stepToGo.set(MapXmlCreator.getMaxGameStep(stepToGo.get(),
            getUnitPlacementsMap().isEmpty() ? GameStep.TERRITORY_OWNERSHIP : GameStep.UNIT_PLACEMENTS));
      }

      @Override
      protected void handlePropertyList() {
        if (!getGameSettingsMap().isEmpty()) {
          stepToGo.set(getNotes().length() > 0 ? GameStep.MAP_FINISHED : GameStep.GAME_SETTINGS);
        }
      }

    });
    return stepToGo.get();
  }

  private static void initializeAll() {
    MapXmlCreator.mapFolderLocation = null;
    MapXmlCreator.mapImageFile = null;
    MapXmlCreator.mapCentersFile = null;
    mapXmlData.initialize();
  }

  ///////////////////////////////////////////
  // Start of XML creation methods
  ///////////////////////////////////////////
  static Document getXmlDocument() throws ParserConfigurationException {
    // TODO: break into smaller chunks, consider builder pattern
    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    final DocumentBuilder db = dbf.newDocumentBuilder();

    final Document doc = db.newDocument();
    final Element game = doc.createElement(XML_NODE_NAME_GAME);
    doc.appendChild(game);

    appendFromXmlStrings(doc, game);

    final Element map = doc.createElement(XML_NODE_NAME_MAP);
    game.appendChild(map);


    map.appendChild(doc.createComment(" Territory Definitions "));

    boolean territoryAttachmentNeeded = appendFromTerritoryDefinitions(doc, map);

    appendFromResourceList(doc, game);

    appendFromPlayerName(doc, game);

    appendFromUnitDefinitions(doc, game, map);

    appendFromGamePlaySequence(doc, game);

    if (!getTerritoyProductionsMap().isEmpty()) {
      territoryAttachmentNeeded = true;
    }

    if (!getTechnologyDefinitionsMap().isEmpty() || !getUnitAttachmentsMap().isEmpty() || territoryAttachmentNeeded
        || !getCanalDefinitionsMap().isEmpty()) {
      final Element attachmentList = doc.createElement(XML_NODE_NAME_ATTACHMENT_LIST);
      game.appendChild(attachmentList);

      appendToAttachmentList(doc, territoryAttachmentNeeded, attachmentList);
    }

    appendInitialize(doc, game);

    return doc;
  }

  private static void appendInitialize(final Document doc, final Element game) {
    final Element initialize = doc.createElement(XML_NODE_NAME_INITIALIZE);
    game.appendChild(initialize);

    appendFromTerritoryOwnerships(doc, initialize);

    appendFromUnitPlacements(doc, initialize);

    appendPropertyList(doc, game);
  }

  private static void appendPropertyList(final Document doc, final Element game) {
    final Element propertyList = doc.createElement(XML_NODE_NAME_PROPERTY_LIST);
    game.appendChild(propertyList);

    appendFromGameSettings(doc, propertyList);

    if (getNotes().length() > 0) {
      final Element property = doc.createElement(XML_NODE_NAME_PROPERTY);
      property.setAttribute(XML_ATTR_PROPERTY_NAME_NAME, XML_ATTR_VALUE_PROPERTY_NAME_NOTES);
      final Element propertyValue = doc.createElement(XML_NODE_NAME_VALUE);
      propertyValue.appendChild(doc.createCDATASection(getNotes()));
      property.appendChild(propertyValue);
      propertyList.appendChild(property);
    }

    if (getMapXmlFile() != null) {
      propertyList.appendChild(doc.createComment(" Map Name: also used for map utils when asked "));
      final Element property = doc.createElement(XML_NODE_NAME_PROPERTY);
      property.setAttribute(XML_ATTR_PROPERTY_NAME_NAME, XML_ATTR_VALUE_PROPERTY_NAME_MAP_NAME);
      final String fileName = getMapXmlFile().getName();
      property.setAttribute(XML_ATTR_PROPERTY_NAME_VALUE, fileName.substring(0, fileName.lastIndexOf(".") - 1));
      property.setAttribute(XML_ATTR_PROPERTY_NAME_EDITABLE, Constants.PROPERTY_FALSE);
      propertyList.appendChild(property);
    }
  }

  private static void appendFromGameSettings(final Document doc, final Element propertyList) {
    if (!getGameSettingsMap().isEmpty()) {
      for (final Entry<String, List<String>> gameSettingsEntry : getGameSettingsMap().entrySet()) {
        final Element property = doc.createElement(XML_NODE_NAME_PROPERTY);
        final List<String> settingsValue = gameSettingsEntry.getValue();
        property.setAttribute(XML_ATTR_PROPERTY_NAME_NAME, gameSettingsEntry.getKey());
        final String valueString = settingsValue.get(0);
        property.setAttribute(XML_ATTR_PROPERTY_NAME_VALUE, valueString);
        property.setAttribute(XML_ATTR_PROPERTY_NAME_EDITABLE, settingsValue.get(1));
        if (valueString.equals(Constants.PROPERTY_TRUE) || valueString.equals(Constants.PROPERTY_FALSE)) {
          property.appendChild(doc.createElement(XML_NODE_NAME_BOOLEAN));
        } else {
          final String minString = settingsValue.get(2);
          final String maxString = settingsValue.get(3);
          try {
            Integer.valueOf(minString);
            Integer.valueOf(maxString);
            final Element number = doc.createElement(XML_NODE_NAME_NUMBER);
            number.setAttribute(XML_ATTR_NUMBER_NAME_MIN, minString);
            number.setAttribute(XML_ATTR_NUMBER_NAME_MAX, maxString);
            property.appendChild(number);
          } catch (final NumberFormatException nfe) {
            // nothing to do
          }
        }
        propertyList.appendChild(property);
      }
    }
  }

  private static void appendFromUnitPlacements(final Document doc, final Element initialize) {
    if (!getUnitPlacementsMap().isEmpty()) {
      final Element unitInitialize = doc.createElement(XML_NODE_NAME_UNIT_INITIALIZE);
      initialize.appendChild(unitInitialize);
      for (final Entry<String, Map<String, Map<String, Integer>>> placementEntry : getUnitPlacementsMap()
          .entrySet()) {
        final Element unitPlacementTemplate = doc.createElement(XML_NODE_NAME_UNIT_PLACEMENT);
        unitPlacementTemplate.setAttribute(XML_ATTR_UNIT_PLACEMENT_NAME_TERRITORY, placementEntry.getKey());
        for (final Entry<String, Map<String, Integer>> playerPlacements : placementEntry.getValue()
            .entrySet()) {
          final Element playerUnitPlacementTemplate = (Element) unitPlacementTemplate.cloneNode(false);
          if (playerPlacements.getKey() != null) {
            playerUnitPlacementTemplate.setAttribute(XML_ATTR_UNIT_PLACEMENT_NAME_OWNER, playerPlacements.getKey());
          }
          for (final Entry<String, Integer> unitDetails : playerPlacements.getValue().entrySet()) {
            if (unitDetails.getValue() > 0) {
              final Element unitPlacement = (Element) playerUnitPlacementTemplate.cloneNode(false);
              unitPlacement.setAttribute(XML_ATTR_ATTACHMENT_NAME_UNIT_TYPE, unitDetails.getKey());
              unitPlacement.setAttribute(XML_ATTR_RESULT_NAME_QUANTITY, unitDetails.getValue().toString());
              unitInitialize.appendChild(unitPlacement);
            }
          }
        }
      }
    }
  }

  private static void appendFromTerritoryOwnerships(final Document doc, final Element initialize) {
    if (!getTerritoryOwnershipsMap().isEmpty()) {
      final Element ownerInitialize = doc.createElement(XML_NODE_NAME_OWNER_INITIALIZE);
      initialize.appendChild(ownerInitialize);
      final Map<String, List<String>> playerTerritories = new HashMap<>();
      for (final String player : getPlayerNames()) {
        playerTerritories.put(player, new ArrayList<>());
      }
      for (final Entry<String, String> ownershipEntry : getTerritoryOwnershipsMap().entrySet()) {
        playerTerritories.get(ownershipEntry.getValue()).add(ownershipEntry.getKey());
      }
      for (final Entry<String, List<String>> playerTerritoriesEntry : playerTerritories.entrySet()) {
        doc.createComment(" " + playerTerritoriesEntry.getKey() + " Owned Territories ");
        final Element territoryOwnerTemplate = doc.createElement(XML_NODE_NAME_TERRITORY_OWNER);
        territoryOwnerTemplate.setAttribute(XML_ATTR_UNIT_PLACEMENT_NAME_OWNER, playerTerritoriesEntry.getKey());
        for (final String territory : playerTerritoriesEntry.getValue()) {
          final Element territoryOwner = (Element) territoryOwnerTemplate.cloneNode(false);
          territoryOwner.setAttribute(XML_ATTR_TERRITORY_OWNER_NAME_TERRITORY, territory);
          ownerInitialize.appendChild(territoryOwner);
        }
      }
    }
  }

  private static void appendFromResourceList(final Document doc, final Element game) {
    if (!getResourceList().isEmpty()) {
      final Element resourceListElement = doc.createElement(XML_NODE_NAME_RESOURCE_LIST);
      game.appendChild(resourceListElement);
      for (final String resourceName : getResourceList()) {
        final Element resourceElement = doc.createElement(XML_NODE_NAME_RESOURCE);
        resourceElement.setAttribute(XML_ATTR_RESOURCE_NAME_NAME, resourceName);
        resourceListElement.appendChild(resourceElement);
      }
    }
  }

  private static void appendToAttachmentList(final Document doc, final boolean territoryAttachmentNeeded,
      final Element attachmentList) {
    final Element attachmentTemplate = doc.createElement(XML_NODE_NAME_ATTACHMENT);
    if (!getTechnologyDefinitionsMap().isEmpty()) {
      setAttachmentTemplateAttributes(attachmentTemplate, Constants.TECH_ATTACHMENT_NAME,
          TechAttachment.class.getName(), XML_ATTR_ATTACHMENT_NAME_PLAYER);
      writeAttachmentNodes(doc, attachmentList, getTechnologyDefinitionsMap(), attachmentTemplate);
    }
    if (!getUnitAttachmentsMap().isEmpty()) {
      setAttachmentTemplateAttributes(attachmentTemplate, Constants.UNIT_ATTACHMENT_NAME,
          UnitAttachment.class.getName(), XML_ATTR_ATTACHMENT_NAME_UNIT_TYPE);
      writeAttachmentNodes(doc, attachmentList, getUnitAttachmentsMap(), attachmentTemplate);
    }
    if (territoryAttachmentNeeded) {
      setAttachmentTemplateAttributes(attachmentTemplate, Constants.TERRITORY_ATTACHMENT_NAME,
          TerritoryAttachment.class.getName(), XML_NODE_NAME_TERRITORY);
      writeAttachmentNodes(doc, attachmentList, getTerritoryAttachments(), attachmentTemplate);
    }
    if (!getCanalDefinitionsMap().isEmpty()) {
      setAttachmentTemplateAttributes(attachmentTemplate, Constants.INF_ATTACHMENT_NAME,
          CanalAttachment.class.getName(), XML_NODE_NAME_TERRITORY);
      addCanalDefinitionsAttachmentNodes(doc, attachmentList, attachmentTemplate);
      writeAttachmentNodes(doc, attachmentList, getUnitAttachmentsMap(), attachmentTemplate);
    }
  }


  private static void setAttachmentTemplateAttributes(final Element attachmentTemplate, final String name,
      final String javaClass, final String player) {
    attachmentTemplate.setAttribute(XML_ATTR_ATTACHMENT_NAME_NAME, name);
    attachmentTemplate.setAttribute(XML_ATTR_ATTACHMENT_NAME_JAVA_CLASS, javaClass);
    attachmentTemplate.setAttribute(XML_ATTR_ATTACHMENT_NAME_TYPE, player);
  }

  private static void addCanalDefinitionsAttachmentNodes(final Document doc, final Element attachmentList,
      final Element attachmentTemplate) {
    for (final Entry<String, CanalTerritoriesTuple> canalDefEntry : getCanalDefinitionsMap()
        .entrySet()) {
      final CanalTerritoriesTuple canalDef = canalDefEntry.getValue();
      final Iterator<String> iter_landTerrs = canalDef.getLandTerritories().iterator();
      final StringBuilder sb = new StringBuilder(iter_landTerrs.next());
      while (iter_landTerrs.hasNext()) {
        sb.append(XML_ATTR_VALUE_SEPARATOR_OPTION_VALUE).append(iter_landTerrs.next());
      }
      final String landTerritories = sb.toString();

      final Element attachmentTemplateCanal = (Element) attachmentTemplate.cloneNode(false);
      final Element canalOptionName = doc.createElement(XML_NODE_NAME_OPTION);
      canalOptionName.setAttribute(XML_ATTR_OPTION_NAME_NAME, XML_ATTR_VALUE_OPTION_NAME_CANAL_NAME);
      canalOptionName.setAttribute(XML_ATTR_PROPERTY_NAME_VALUE, canalDefEntry.getKey());
      attachmentTemplateCanal.appendChild(canalOptionName);
      final Element canalOptionLandTerrs = doc.createElement(XML_NODE_NAME_OPTION);
      canalOptionLandTerrs.setAttribute(XML_ATTR_PROPERTY_NAME_NAME, XML_ATTR_VALUE_OPTION_NAME_LAND_TERRITORIES);
      canalOptionLandTerrs.setAttribute(XML_ATTR_PROPERTY_NAME_VALUE, landTerritories);
      attachmentTemplateCanal.appendChild(canalOptionLandTerrs);
      for (final String waterTerr : canalDef.getWaterTerritories()) {
        final Element canalAttachment = (Element) attachmentTemplateCanal.cloneNode(true);
        canalAttachment.setAttribute(XML_ATTR_ATTACHMENT_NAME_ATTACH_TO, waterTerr);
        attachmentList.appendChild(canalAttachment);
      }
    }
  }

  private static Map<String, List<String>> getTerritoryAttachments() {
    final Map<String, List<String>> territoryAttachments = Maps.newLinkedHashMap();
    addAttachmentsFromTerritoryDefinitions(territoryAttachments);
    addAttachmentsFromTerritoryProductions(territoryAttachments);
    return territoryAttachments;
  }

  private static void addAttachmentsFromTerritoryProductions(final Map<String, List<String>> territoryAttachments) {
    for (final Entry<String, Integer> productionEntry : getTerritoyProductionsMap().entrySet()) {
      final int production = productionEntry.getValue();
      if (production > 0) {
        final String territoryName = productionEntry.getKey();
        final List<String> attachmentOptions = new ArrayList<>();
        attachmentOptions.add(territoryName);
        attachmentOptions.add(Integer.toString(production));
        territoryAttachments.put("production_" + territoryName, attachmentOptions);
      }
    }
  }

  private static void addAttachmentsFromTerritoryDefinitions(final Map<String, List<String>> territoryAttachments) {
    for (final Entry<String, Map<Definition, Boolean>> territoryDefinition : getTerritoryDefintionsMap().entrySet()) {
      final String territoryName = territoryDefinition.getKey();
      for (final Entry<Definition, Boolean> definition : territoryDefinition.getValue().entrySet()) {
        if (definition.getValue() == Boolean.TRUE) {
          final List<String> attachmentOptions = new ArrayList<>();
          attachmentOptions.add(territoryName);
          attachmentOptions.add("true");
          // TODO: handle capital different based on owner
          // owner defined (step 13) only after territory definitions (step 2)
          // <option name="capital" value="Italians"/>
          territoryAttachments.put(
              TerritoryDefinitionDialog.getDefinitionString(definition.getKey()) + "_" + territoryName,
              attachmentOptions);
        }
      }
    }
  }

  private static boolean appendFromTerritoryDefinitions(final Document doc, final Element map) {
    boolean territoryAttachmentNeeded = false;
    for (final Entry<String, Map<Definition, Boolean>> entryTerritoryDefinition : getTerritoryDefintionsMap()
        .entrySet()) {
      final Element territory = doc.createElement(XML_NODE_NAME_TERRITORY);
      territory.setAttribute(XML_ATTR_PROPERTY_NAME_NAME, entryTerritoryDefinition.getKey());
      final Map<Definition, Boolean> territoryDefinition = entryTerritoryDefinition.getValue();
      final int territoryDefinitionSize = territoryDefinition.size();
      final Boolean isWater = territoryDefinition.get(Definition.IS_WATER);
      if (isWater != null && isWater) {
        territory.setAttribute(TerritoryDefinitionDialog.getDefinitionString(Definition.IS_WATER), "true");
        if (territoryDefinitionSize > 1) {
          territoryAttachmentNeeded = true;
        }
      } else if (territoryDefinitionSize > 1 || isWater == null && territoryDefinitionSize > 0) {
        territoryAttachmentNeeded = true;
      }
      map.appendChild(territory);
    }

    map.appendChild(doc.createComment(" Territory Connections "));
    for (final Entry<String, Set<String>> entryTerritoryConnection : getTerritoryConnectionsMap().entrySet()) {
      final Element connectionTemp = doc.createElement(XML_NODE_NAME_CONNECTION);
      connectionTemp.setAttribute(XML_ATTR_CONNECTION_NAME_T1, entryTerritoryConnection.getKey());
      for (final String t2 : entryTerritoryConnection.getValue()) {
        connectionTemp.setAttribute(XML_ATTR_CONNECTION_NAME_T2, t2);
        map.appendChild(connectionTemp.cloneNode(false));
      }
    }
    return territoryAttachmentNeeded;
  }

  private static void appendFromXmlStrings(final Document doc, final Element game) {
    Element currentElem = null;
    String prevKeyNode = null;
    for (final Entry<String, String> entryXmlString : getXmlStringsMap().entrySet()) {
      final String key = entryXmlString.getKey();
      final String[] split = key.split("_@");
      if (!split[0].equals(prevKeyNode)) {
        Element parentElem = game;
        for (final String newNode : split[0].split("_")) {
          currentElem = doc.createElement(newNode);
          parentElem.appendChild(currentElem);
          parentElem = currentElem;
        }
      }
      currentElem.setAttribute(split[1], entryXmlString.getValue());
      prevKeyNode = split[0];
    }
  }

  private static void appendFromGamePlaySequence(final Document doc, final Element game) {
    if (!getGamePlaySequenceMap().isEmpty()) {
      final Element gamePlay = doc.createElement(XML_NODE_NAME_GAME_PLAY);
      game.appendChild(gamePlay);

      for (final Entry<String, List<String>> delegateStep : getGamePlaySequenceMap().entrySet()) {
        final List<String> delegateProperties = delegateStep.getValue();
        final Element delegate = doc.createElement(XML_NODE_NAME_DELEGATE);
        delegate.setAttribute(XML_ATTR_PROPERTY_NAME_NAME, delegateStep.getKey());
        delegate.setAttribute(XML_ATTR_ATTACHMENT_NAME_JAVA_CLASS,
            TRIPLEA_JAVA_CLASS_DELEGATE_PATH + delegateProperties.get(0));
        delegate.setAttribute(XML_ATTR_STEP_NAME_DISPLAY, delegateProperties.get(1));
        gamePlay.appendChild(delegate);
      }

      if (!getPlayerSequenceMap().isEmpty()) {
        final Element sequence = doc.createElement(XML_NODE_NAME_SEQUENCE);
        gamePlay.appendChild(sequence);

        for (final Entry<String, Triple<String, String, Integer>> sequenceStep : getPlayerSequenceMap().entrySet()) {
          final Triple<String, String, Integer> sequenceProperties = sequenceStep.getValue();
          final Element step = doc.createElement(XML_NODE_NAME_STEP);
          final String sequenceStepKey = sequenceStep.getKey();
          step.setAttribute(XML_ATTR_STEP_NAME_NAME, sequenceStepKey);
          step.setAttribute(XML_ATTR_STEP_NAME_DELEGATE, sequenceProperties.getFirst());
          if (sequenceProperties.getSecond().length() > 0) {
            step.setAttribute(XML_ATTR_ATTACHMENT_NAME_PLAYER, sequenceProperties.getSecond());
          }
          final int maxRunCount = sequenceProperties.getThird();
          if (maxRunCount > 0) {
            step.setAttribute(XML_ATTR_STEP_NAME_MAX_RUN_COUNT, Integer.toString(maxRunCount));
          }
          if (stepNameIndicatesNonCombatMove(sequenceStepKey)) {
            step.setAttribute(XML_ATTR_STEP_NAME_DISPLAY, "Non Combat Move");
          }
          sequence.appendChild(step);
        }
      }
    }
  }

  // TODO REPLACE 'endsWith("NonCombatMove")'-checks IN REMAINING PROJEKT
  /**
   * @param stepName - string of the step name.
   * @return true if string ends with "NonCombatMove", false otherwise
   */
  private static boolean stepNameIndicatesNonCombatMove(final String stepName) {
    return stepName.endsWith("NonCombatMove");
  }

  private static void appendFromPlayerName(final Document doc, final Element game) {
    if (!getPlayerNames().isEmpty()) {
      final Element playerList = doc.createElement(XML_NODE_NAME_PLAYER_LIST);
      game.appendChild(playerList);
      playerList.appendChild(doc.createComment(" In Turn Order "));
      appendChildrenWithTwoAttributesFromList(doc, playerList, getPlayerNames(), XML_NODE_NAME_PLAYER,
          XML_ATTR_PLAYER_NAME_NAME, XML_ATTR_PLAYER_NAME_OPTIONAL, XML_ATTR_VALUE_PLAYER_OPTIONAL_NAME_FALSE);

      final Map<String, List<String>> alliances = Maps.newHashMap();
      for (final Entry<String, String> allianceEntry : getPlayerAllianceMap().entrySet()) {
        final String allianceName = allianceEntry.getValue();
        List<String> players = alliances.get(allianceName);
        if (players == null) {
          players = new ArrayList<>();
          alliances.put(allianceName, players);
        }
        players.add(allianceEntry.getKey());
      }
      for (final Entry<String, List<String>> allianceEntry : alliances.entrySet()) {
        final String allianceName = allianceEntry.getKey();
        playerList.appendChild(doc.createComment(" " + allianceName + " Alliance "));
        appendChildrenWithTwoAttributesFromList(doc, playerList, allianceEntry.getValue(), XML_NODE_NAME_ALLIANCE,
            XML_ATTR_ATTACHMENT_NAME_PLAYER,
            XML_NODE_NAME_ALLIANCE, allianceName);
      }
    }
  }

  private static void appendFromUnitDefinitions(final Document doc, final Element game, final Element map) {
    if (!getUnitDefinitionsMap().isEmpty()) {
      map.appendChild(doc.createComment(" Unit Definitions "));
      final Element unitList = doc.createElement(XML_NODE_NAME_UNIT_LIST);
      game.appendChild(unitList);

      final Element production = doc.createElement(XML_NODE_NAME_PRODUCTION);
      game.appendChild(production);
      final String firstResourceName = getResourceList().get(0);
      for (final Entry<String, List<Integer>> unitDefinition : getUnitDefinitionsMap().entrySet()) {
        final String unitName = unitDefinition.getKey();

        final Element unit = doc.createElement(XML_NODE_NAME_UNIT);
        unit.setAttribute(XML_ATTR_PROPERTY_NAME_NAME, unitName);
        unitList.appendChild(unit);

        final List<Integer> definition = unitDefinition.getValue();
        final Element productionRule = doc.createElement(XML_NODE_NAME_PRODUCTION_RULE);
        productionRule.setAttribute(XML_ATTR_PROPERTY_NAME_NAME, getProductionRuleAttrNameValue(unitName));
        production.appendChild(productionRule);
        final Element prCost = doc.createElement(XML_NODE_NAME_COST);
        prCost.setAttribute(XML_ATTR_COST_NAME_RESOURCE, firstResourceName);
        prCost.setAttribute(XML_ATTR_RESULT_NAME_QUANTITY, definition.get(0).toString());
        productionRule.appendChild(prCost);
        final Element prResult = doc.createElement(XML_NODE_NAME_RESULT);
        prResult.setAttribute(XML_ATTR_RESULT_NAME_RESOURCE_OR_UNIT, unitName);
        prResult.setAttribute(XML_ATTR_RESULT_NAME_QUANTITY, definition.get(1).toString());
        productionRule.appendChild(prResult);
      }

      for (final Entry<String, List<String>> productionFrontierEntry : getProductionFrontiersMap().entrySet()) {
        final String productionFrontierName = productionFrontierEntry.getKey();

        final Element productionFrontier = doc.createElement(XML_NODE_NAME_PRODUCTION_FRONTIER);
        productionFrontier.setAttribute(XML_ATTR_PROPERTY_NAME_NAME, productionFrontierName);
        production.appendChild(productionFrontier);

        for (final String frontierRuleName : productionFrontierEntry.getValue()) {
          final Element frontierRule = doc.createElement(XML_NODE_NAME_FRONTIER_RULES);
          frontierRule.setAttribute(XML_ATTR_PROPERTY_NAME_NAME, getFrontierRuleValue(frontierRuleName));
          productionFrontier.appendChild(frontierRule);
        }
      }
    }
  }

  private static String getFrontierRuleValue(final String frontierRuleName) {
    return "buy" + frontierRuleName;
  }

  private static String getProductionRuleAttrNameValue(final String unitName) {
    return "buy" + unitName.substring(0, 1).toUpperCase() + unitName.substring(1);
  }

  /**
   * Appends new Elements to parentElement from list having two attributes.
   * Attribute 1 is coming from the provided list
   * Attribute 2 has a fix value for all new elements
   */
  public static void appendChildrenWithTwoAttributesFromList(final Document doc, final Element parentElement,
      final List<String> list, final String childName, final String attrOneKey, final String attrTwoKey,
      final String attrTwoFixValue) {
    for (final String attrOneValue : list) {
      final Element newChild = doc.createElement(childName);
      newChild.setAttribute(attrOneKey, attrOneValue);
      newChild.setAttribute(attrTwoKey, attrTwoFixValue);
      parentElement.appendChild(newChild);
    }
  }

  protected static void writeAttachmentNodes(final Document doc, final Element attachmentList,
      final Map<String, List<String>> hashMap, final Element attachmentTemplate) {
    final Map<String, List<Element>> playerAttachOptions = Maps.newHashMap();
    for (final Entry<String, List<String>> technologyDefinition : hashMap.entrySet()) {
      final List<String> definitionValues = technologyDefinition.getValue();
      final Element option = doc.createElement(XML_NODE_NAME_OPTION);
      final String techKey = technologyDefinition.getKey();
      option.setAttribute(XML_ATTR_PROPERTY_NAME_NAME,
          techKey.substring(0, techKey.lastIndexOf(definitionValues.get(0)) - 1));
      option.setAttribute(XML_ATTR_PROPERTY_NAME_VALUE, definitionValues.get(1));
      final String playerName = definitionValues.get(0);
      List<Element> elementList = playerAttachOptions.get(playerName);
      if (elementList == null) {
        elementList = new ArrayList<>();
        playerAttachOptions.put(playerName, elementList);
      }
      elementList.add(option);
    }
    for (final Entry<String, List<Element>> optionElementList : playerAttachOptions.entrySet()) {
      final String playerName = optionElementList.getKey();
      final Element attachment = (Element) attachmentTemplate.cloneNode(false);
      attachment.setAttribute(XML_ATTR_ATTACHMENT_NAME_ATTACH_TO, playerName);
      attachmentList.appendChild(attachment);
      for (final Element option : optionElementList.getValue()) {
        attachment.appendChild(option);
      }
    }
  }

  public static final String playerNeutral = "<Neutral>";

  static String[] getPlayersListInclNeutral() {
    getPlayerNames().add(playerNeutral);
    final String[] rVal = getPlayerNames().toArray(new String[getPlayerNames().size()]);
    getPlayerNames().remove(playerNeutral);
    return rVal;
  }

  private static void filterForWaterTerrsWithAtLeastOneWaterNeighbor(final Set<String> waterTerrs2) {
    final Set<String> waterTerrs2Copy = new TreeSet<>(waterTerrs2);
    for (final Iterator<String> iter_waterTerr2 = waterTerrs2.iterator(); iter_waterTerr2.hasNext();) {
      final String waterTerr2 = iter_waterTerr2.next();
      waterTerrs2Copy.remove(waterTerr2);
      final Set<String> waterTerrs2ReqNeighbors = new TreeSet<>(waterTerrs2Copy);
      final Set<String> waterTerr2Neightbors = getTerritoryConnectionsMap().get(waterTerr2);
      if (waterTerr2Neightbors != null) {
        for (final String waterTerr2Neighbor : waterTerr2Neightbors) {
          waterTerrs2ReqNeighbors.remove(waterTerr2Neighbor);
        }
      }
      if (!waterTerrs2ReqNeighbors.isEmpty()) {
        iter_waterTerr2.remove();
      }
    }
  }

  private static void validateAndAddCanalDefinition(final String landTerr, final Set<String> waterTerrs,
      final Set<String> landTerrNeighbors, final Entry<String, Set<String>> landWaterTerrConn2) {
    final String landTerr2 = landWaterTerrConn2.getKey();

    Set<String> landTerrNeighbors2 = getTerritoryConnectionsMap().get(landTerr2);
    if (landTerrNeighbors2 == null) {
      landTerrNeighbors2 = Sets.newLinkedHashSet();
    }

    if (!landTerrNeighbors.contains(landTerr2) && !landTerrNeighbors2.contains(landTerr)) {
      return;
    }
    final Set<String> waterTerrs2 = new TreeSet<>(landWaterTerrConn2.getValue());
    waterTerrs2.retainAll(waterTerrs);
    if (waterTerrs2.size() > 1) {
      filterForWaterTerrsWithAtLeastOneWaterNeighbor(waterTerrs2);
      // create canal only if at least 2 water territories remain
      if (waterTerrs2.size() > 1) {
        final Set<String> newLandSet = new TreeSet<>();
        newLandSet.add(landTerr);
        newLandSet.add(landTerr2);
        final CanalTerritoriesTuple terrTuple =
            new CanalTerritoriesTuple(new TreeSet<>(waterTerrs2), newLandSet);
        putCanalDefinitions("Canal" + getCanalDefinitionsMap().size(), terrTuple);
      }
    }
  }

  static void validateAndAddCanalDefinitions(final Map<String, Set<String>> landWaterTerritoyConnections) {
    final Map<String, Set<String>> landWaterTerrConnChecks =
        Maps.newHashMap(landWaterTerritoyConnections);
    for (final Entry<String, Set<String>> landWaterTerrConn : landWaterTerritoyConnections.entrySet()) {
      final String landTerr = landWaterTerrConn.getKey();
      final Set<String> waterTerrs = landWaterTerrConn.getValue();
      Set<String> landTerrNeighbors = getTerritoryConnectionsMap().get(landTerr);
      if (landTerrNeighbors == null) {
        landTerrNeighbors = Sets.newLinkedHashSet();
      }
      landWaterTerrConnChecks.remove(landTerr);
      for (final Entry<String, Set<String>> landWaterTerrConn2 : landWaterTerrConnChecks.entrySet()) {
        validateAndAddCanalDefinition(landTerr, waterTerrs, landTerrNeighbors, landWaterTerrConn2);
      }
    }
  }

  /**
   * @return HTML string listing the canal definitions in the following format:
   *         ' - &lt;canal name>: &lt;water territory 1>-&lt;water territory 2>- ...'
   */
  public static String getHtmlStringFromCanalDefinitions() {
    final StringBuilder sb = new StringBuilder();
    sb.append("<html>The following ").append(getCanalDefinitionsMap().size()).append(" canals have been build:");
    for (final Entry<String, CanalTerritoriesTuple> canalDef : getCanalDefinitionsMap()
        .entrySet()) {
      sb.append(CanalDefinitionsPanel.HTML_CANAL_KEY_PREFIX).append(canalDef.getKey())
          .append(CanalDefinitionsPanel.HTML_CANAL_KEY_POSTFIX);
      sb.append(Joiner.on("-").join(canalDef.getValue().getWaterTerritories().iterator()));
    }
    sb.append("</html>");
    return sb.toString();
  }

  static class Wrapper<T> {
    private T object;

    Wrapper(T object) {
      this.object = object;
    }

    void set(T object) {
      this.object = object;
    }

    T get() {
      return object;
    }
  }
}
