package examples;

import java.util.ArrayList;
import java.util.List;

public class Requestor {
    public Requestor(SocialGraph socialGraph, UserDetailsService userDetailsService) {
        this.socialGraph = socialGraph;
        this.userDetailsService = userDetailsService;
    }

    private final SocialGraph socialGraph;
    private final UserDetailsService userDetailsService;

    public void lookUpFriends(long userId) {
        List<Long> friendIds = socialGraph.query(userId);
        List<User> friends = new ArrayList<>();

        for (Long friendId : friendIds) {
            friends.add(userDetailsService.lookup(friendId));
        }
    }
}