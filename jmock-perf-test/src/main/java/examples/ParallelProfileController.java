package examples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParallelProfileController {
    public ParallelProfileController(SocialGraph socialGraph, UserDetailsService userDetailsService) {
        this.socialGraph = socialGraph;
        this.userDetailsService = userDetailsService;
    }

    private final SocialGraph socialGraph;
    private final UserDetailsService userDetailsService;

    public void lookUpFriends(long userId) {
        List<Long> friendIds = socialGraph.query(userId);

        ExecutorService es = Executors.newFixedThreadPool(2);

        List<Future<User>> userDetailsRequests = new ArrayList<>();

        for (Long friend : friendIds) {
            userDetailsRequests.add(es.submit(() -> userDetailsService.lookup(friend)));
        }
        es.shutdown();

        List<User> friends = new ArrayList<>();

        for (Future<User> userDetailsRequest : userDetailsRequests) {
            try {
                friends.add(userDetailsRequest.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}