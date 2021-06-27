import lombok.Flyweight;

@Flyweight
public class Permissions {
@Flyweight.Object
int read;
@Flyweight.Object
int write;
@Flyweight.Object
int execute;
}
