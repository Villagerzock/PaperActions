[![Latest Snapshot](https://repo.qouve.space/api/badge/latest/snapshots/net/villagerzock/PaperActions?name=PaperActions&prefix=v)](https://repo.qouve.space/#/snapshots/net/villagerzock)
# PaperActions

PaperActions is a lightweight library for Paper that allows plugins to expose and execute actions in a standardized way.

Instead of depending directly on other plugins, plugins can register actions that any other plugin can discover and execute.

---

# Features

* Annotation-based action registration
* Automatic parameter discovery
* Named parameters
* Cross-plugin action execution
* Lightweight and easy to integrate
* Minimal setup

---

# Getting Started

## Registering Actions

Register your action classes during plugin startup.

```java
@Override
public void onEnable() {
	ActionManager.getInstance(this)
			.register(MyActions.class);
}
```

## Creating Actions

Create actions using the `@Action` annotation.

```java
@Action("heal")
public static void heal(Player player) {
	player.setHealth(player.getMaxHealth());
}
```

## Executing Actions

Execute actions using the `ActionManager`.

```java
ActionManager.getInstance(plugin).executeAction(
	"example:heal",
	Map.of(
		"player", player
	)
);
```

---

# Developer Guide

PaperActions is designed to allow completely unrelated plugins to communicate with one another.

To maximize compatibility, plugin developers are encouraged to follow the conventions below.

## Conventions

### Standard Parameter Names

Whenever an action expects one of the following concepts, use these parameter names whenever possible.

| Parameter  | Description                                      |
| ---------- | ------------------------------------------------ |
| `player`   | The player executing or owning the action.       |
| `target`   | The entity or player the action is performed on. |
| `location` | The location where the action should happen.     |
| `world`    | The world involved in the action.                |
| `plugin`   | The plugin requesting or executing the action.   |

For example:

```java
@Action("heal")
public static void heal(Player player) {
	...
}
```

Using consistent parameter names allows other plugins to automatically provide matching values without requiring custom integrations.

---

### Using `@Param`

By default, PaperActions uses the Java parameter name as the public parameter name of an action.

If a parameter is annotated with `@Param` and a `name` is specified, that name overrides the Java parameter name.

```java
@Action("heal")
public static void heal(
	@Param(name = "player") Player p
) {
	...
}
```

Although the Java variable is named `p`, the action exposes the parameter as `player`.

This is especially useful when:

* You prefer short or internal variable names.
* You want to expose a different public parameter name.
* Your project is compiled **without** the `-parameters` compiler option.

Without `-parameters`, Java does not retain parameter names at runtime. In that case, PaperActions cannot automatically determine the original parameter names. Using `@Param` ensures that the intended public parameter name is always available.

---

### Conventions Are Recommendations

The conventions described above are **recommendations**, not requirements.

Action providers are free to expose any parameter names they choose. However, following the recommended names greatly improves interoperability between plugins and enables automatic parameter injection.

Action executors should not assume that every action follows these conventions. If possible, they should allow server owners to provide additional parameters or remap parameter names through configuration.

For example, a GUI plugin could allow actions to be configured like this:

```yaml
slots:
  0:
    action: myplugin:heal
    parameters:
      player: clicker
      amount: 20
```

In this example, the GUI plugin provides the player who clicked the item as the `player` parameter and also supplies an additional `amount` parameter. This allows the same action to be reused in many different contexts without requiring hardcoded integrations between plugins.

---

### Keep Actions Stateless

Actions should behave like commands.

Avoid relying on temporary or shared internal state whenever possible. Every piece of information required to execute an action should be supplied through parameters.

---

### Prefer Generic Types

If an action is intended to be used by other plugins, prefer Bukkit or Paper API types over plugin-specific classes.

Examples include:

* `Player`
* `Entity`
* `Location`
* `World`
* `ItemStack`

Using common API types makes actions compatible with a wider range of plugins.

---

### Choose Stable Action Names

Action names become part of your plugin's public API.

Avoid renaming or removing actions unnecessarily, as other plugins may already depend on them.

---

# License

PaperActions is licensed under the GPL-3.0 License.
