package service;


import model.Match;
import model.Tournament;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.util.*;

@RestController()
public class ColombianFootballController {

    private enum TOURNAMENT_TYPE { LIGA_APERTURA, LIGA_CLAUSURA, TORNEO_APERTURA, TORNEO_CLAUSURA }
    private static final Map<TOURNAMENT_TYPE, String> ID_BY_TOURNAMENT;

    static {
        ID_BY_TOURNAMENT = new HashMap<>();
        ID_BY_TOURNAMENT.put(TOURNAMENT_TYPE.LIGA_APERTURA, "371");
        ID_BY_TOURNAMENT.put(TOURNAMENT_TYPE.LIGA_CLAUSURA, "589");
        ID_BY_TOURNAMENT.put(TOURNAMENT_TYPE.TORNEO_APERTURA, "625");
        ID_BY_TOURNAMENT.put(TOURNAMENT_TYPE.TORNEO_CLAUSURA, "901");
    }

    private RestClient dimayorRestClient;
    private RestClient citiesRestClient;

    public ColombianFootballController() throws IOException {
        Properties prop = PropertiesLoaderUtils.loadAllProperties("main.properties");
        dimayorRestClient = new RestClient(prop.getProperty("dimayorServer"));
        citiesRestClient = new RestClient(prop.getProperty("citiesServer"));
    }

    @RequestMapping(
            value = "/todosLosPartidos/{year}/torneo/{tournament}",
            method = RequestMethod.GET,
            produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Tournament> getAllMatches(@PathVariable("year") int year, @PathVariable("tournament") TOURNAMENT_TYPE tournamentType) {
        Tournament tournament = buildTournmament(tournamentType, year, Optional.empty());
        if (tournament == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(tournament, HttpStatus.OK);
    }

    @RequestMapping(
            value = "/todosLosPartidos/{year}/torneo/{tournament}/fecha/{round}",
            method = RequestMethod.GET,
            produces = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<Tournament> getAllMatchesByRound(@PathVariable("year") int year, @PathVariable("tournament") TOURNAMENT_TYPE tournamentType, @PathVariable("round") int round) {
        Tournament tournament = buildTournmament(tournamentType, year, Optional.of(round));
        if (tournament == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(tournament, HttpStatus.OK);
    }

    private Tournament buildTournmament(TOURNAMENT_TYPE tournamentType, int year, Optional<Integer> round) {
        String tournamentId =  ID_BY_TOURNAMENT.get(tournamentType);
        if (tournamentId == null) {
            return null;
        }
        JSONObject allMatches;
        try {
            allMatches = new JSONObject(dimayorRestClient.get("summary/" + tournamentId + "/" + year + "/all.json"));
        } catch(HttpClientErrorException e) {
            return null;
        }
        Tournament tournament = new Tournament();
        tournament.setName((String) allMatches.getJSONObject("competition").get("name"));
        tournament.setYear((Integer) allMatches.getJSONObject("competition").get("season_id"));
        tournament.setMatches(new ArrayList<>());
        JSONObject phases = allMatches.getJSONObject("phases");
        for (String phaseId : phases.keySet()) {
            JSONObject phase = phases.getJSONObject(phaseId);
            Object rounds = phase.get("rounds");
            Iterator roundsIt;
            if (rounds instanceof JSONArray) {
                roundsIt = ((JSONArray) rounds).iterator();
            } else {
                roundsIt = ((JSONObject) rounds).keys();
            }
            for (Iterator it = roundsIt; it.hasNext(); ) {
                Object roundId = it.next();
                JSONObject allMatchesInRound;
                try {
                    allMatchesInRound = new JSONObject(dimayorRestClient.get("schedules/" + tournamentId + "/" + year + "/rounds/" + roundId + ".json"));
                } catch(HttpClientErrorException e) {
                    return null;
                }
                Object matches = allMatchesInRound.get("matches");
                Iterator matchesIt;
                if (matches instanceof JSONArray) {
                    matchesIt = ((JSONArray) matches).iterator();
                } else {
                    matchesIt = ((JSONObject) matches).keys();
                }
                int currentRound = (Integer) allMatchesInRound.getJSONObject("round").get("number");
                if (round.isPresent() && round.get() != currentRound) {
                    continue;
                }
                for (Iterator it1 = matchesIt; it1.hasNext(); ) {
                    String matchId = (String) it1.next();
                    JSONObject match =  ((JSONObject) matches).getJSONObject(matchId);

                    Match newMatch = new Match();
                    newMatch.setLocal((String) match.getJSONObject("home").get("name"));
                    newMatch.setVisitor((String) match.getJSONObject("away").get("name"));
                    newMatch.setMatchDate(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss Z").parseDateTime(match.get("date") + " -05:00"));
                    newMatch.setStadium((String) match.get("venue_name"));
                    newMatch.setRound(currentRound);

                    String cityByStadium;
                    try {
                        cityByStadium = citiesRestClient.get("city/stadium/" + match.get("venue_name"));
                    } catch(HttpClientErrorException e) {
                        cityByStadium = "";
                    }
                    newMatch.setCity(cityByStadium);
                    tournament.getMatches().add(newMatch);
                }
            }
        }
        tournament.getMatches().sort(Comparator.comparing(Match::getComparatorTime));
        return tournament;
    }

}
