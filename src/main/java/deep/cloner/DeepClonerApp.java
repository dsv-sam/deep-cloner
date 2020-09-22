package deep.cloner;

import deep.cloner.data.Man;
import deep.cloner.utils.CloneUtils;

import java.util.ArrayList;
import java.util.List;

public class DeepClonerApp {

    public static void main(String[] args) {
        List<String> favoriteBooks = new ArrayList<>(List.of("book1", "book2", "book3"));
        Man sourceMan = new Man("Man", 1, favoriteBooks);
        System.out.println("source: " + sourceMan);


        Man cloneMan = CloneUtils.cloneObject(sourceMan);
        System.out.println("clone: " + cloneMan);
    }
}