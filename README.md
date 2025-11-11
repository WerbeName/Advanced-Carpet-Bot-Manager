# Advanced Carpet Bot Manager

A comprehensive Minecraft Fabric mod that provides safe and controlled bot management for all players while maintaining server performance and preventing abuse. Compatible with **Minecraft 1.21.10+** and the latest Fabric Carpet mod.

## 🎯 **Key Features**

- **🤖 User-Friendly Bot Management**: Simple `/cbr` commands for spawning and despawning bots
- **🏷️ Automatic Bot Naming**: Bots are automatically named `bot_playername_1`, `bot_playername_2`, etc.
- **🛡️ Smart Permission System**: Works without plugins, with optional LuckPerms integration
- **⚡ Operator Privileges**: OPs can bypass bot limits for server administration
- **🔒 Bot Ownership Tracking**: Players can only manage their own bots
- **📊 Configurable Limits**: Set different bot limits per player or globally
- **🚫 Real Player Protection**: Prevents creating bots with names of existing players
- **🎯 Smart Tab Completion**: Shows relevant players and bots for easy management

## 🚀 **Quick Start**

### Prerequisites
1. **Minecraft 1.21.10+** with **Fabric Loader 0.17.3+**
2. **Fabric API** (latest version)
3. **Carpet Mod 1.4.186+** (automatically downloaded from JitPack)

### Installation
1. Download the latest `.jar` file from [releases]
2. Place it in your server's or client's `mods/` folder
3. Start Minecraft - the mod will automatically download required dependencies
4. **No additional setup required** - works out of the box!

## 📋 **Commands**

### 🙋 User Commands (Available to everyone)
```
/cbr spawn                          # Spawns your next bot (bot_yourname_1, bot_yourname_2, etc.)
/cbr despawn <bot_name>             # Despawns a specific bot you own
/cbr despawn                        # Despawns all your bots
```

### ⚙️ Admin Commands (Requires OP level 2+)
```
/cbr defaultMaxBots <number>                    # Set global bot limit (default: 2)
/cbr removeOnDisconnect <true/false>            # Auto-remove bots when player leaves
/cbr player <player> maxBots set <number>       # Set per-player bot limit
/cbr player <player> maxBots unset              # Remove per-player limit (use default)
```

### 🔓 Permission Levels

| Player Type         | Bot Limit             | Can Use Commands | Can Bypass Limits |
|---------------------|-----------------------|------------------|-------------------|
| **Regular Player**  | Enforced (default: 2) |  Yes             |  No               |
| **Operator (OP 4)** | **Unlimited**         |  Yes             |  Yes              |
| **Admin (OP 2)**    | Enforced              |  Yes + Config    |  No               |

## 🔐 **Permission System**

### 🎮 Built-in Permission Levels (No plugins required!)

The mod uses Minecraft's native permission system and **works perfectly without any permission plugins**:

- **👤 Regular Players**: Can spawn/manage their own bots within limits
- **🛠️ Admins (OP 2+)**: Can configure bot limits and settings
- **⚡ Operators (OP 4)**: Can bypass all bot limits for server administration

### 🔌 LuckPerms Integration (Optional)

For advanced permission management, the mod supports LuckPerms with these nodes:

#### **User Permissions** (Default: `true`)
```
carpetbotrestriction.user.create_own        # Spawn own bots
carpetbotrestriction.user.manipulate_own    # Manage own bots
carpetbotrestriction.user.shadow            # Use shadow command
```

#### **Admin Permissions** (Default: OP level 2+)
```
carpetbotrestriction.config                 # Use config commands
carpetbotrestriction.admin.create_unlimited # Bypass bot limits
carpetbotrestriction.admin.manipulate_all   # Manage all bots
```

### 📝 Permission Examples

**Remove bot access from a player:**
```bash
/lp user <player> permission set carpetbotrestriction.user.create_own false
```

**Give unlimited bots to a trusted player:**
```bash
/lp user <player> permission set carpetbotrestriction.admin.create_unlimited true
```

## ⚙️ **Configuration**

The mod automatically creates a config file at `config/carpetbotrestriction/config.toml`:

```toml
# Global bot limit for all players (operators can bypass this)
defaultMaxBots = 2

# Whether to remove a player's bots when they disconnect
removeBotsOnDisconnect = false
```

### 📊 Per-Player Limits
You can set individual limits for specific players using commands:

```bash
# Set Steve to have 5 bots instead of the default 2
/cbr player Steve maxBots set 5

# Remove Steve's custom limit (back to default 2)
/cbr player Steve maxBots unset
```

These settings are automatically saved and persist across server restarts.

## 🤖 **How Bot Management Works**

### 🏷️ Automatic Naming
- **Player**: `Steve`
- **First bot**: `bot_steve_1`
- **Second bot**: `bot_steve_2`
- **Third bot**: `bot_steve_3`

The mod automatically handles:
- ✅ Lowercase conversion (Minecraft requirement)
- ✅ Sequential numbering (finds next available slot)
- ✅ Name collision prevention
- ✅ Cross-session persistence

### 🛡️ Ownership & Security
- **👤 Bot Ownership**: Each bot is tied to its creator
- **🔒 Access Control**: Players can only manage their own bots
- **⛔ Protection**: Cannot create bots with real player names
- **🎯 Smart Limits**: Regular players respect limits, OPs can bypass

### � Tab Completion
When typing `/cbr despawn [TAB]`, you'll see:
- Your bot names: `bot_steve_1`, `bot_steve_2`
- **Not** other players' bots (security)
- Only bots that are currently online

## 📖 **Usage Examples**

### 👤 Regular Player Examples
```bash
/cbr spawn                    # Creates bot_steve_1
/cbr spawn                    # Creates bot_steve_2
/cbr spawn                    # ❌ ERROR: "You cannot have more than 2 bots"
/cbr despawn bot_steve_1      # ✅ Removes first bot
/cbr spawn                    # ✅ Creates bot_steve_3 (reuses numbers)
/cbr despawn                  # ✅ Removes ALL your bots
```

### ⚡ Operator Examples
```bash
# OPs can bypass limits
/cbr spawn                    # Creates bot_admin_1
/cbr spawn                    # Creates bot_admin_2
/cbr spawn                    # ✅ Creates bot_admin_3 (no limit!)
/cbr spawn                    # ✅ Creates bot_admin_4 (unlimited)

# OPs can configure limits for others
/cbr defaultMaxBots 1         # Now regular players get only 1 bot
/cbr player Steve maxBots set 10   # But Steve gets 10 bots
```

### 🛠️ Admin Configuration Examples
```bash
# Server-wide settings
/cbr defaultMaxBots 5                    # Everyone gets 5 bots by default
/cbr removeOnDisconnect true             # Clean up when players leave

# Per-player customization
/cbr player Steve maxBots set 10         # Steve gets special treatment
/cbr player Griefer maxBots set 0        # Griefer gets no bots
/cbr player VIP maxBots unset            # VIP back to default (5)
```

## 🛠️ **Compatibility & Requirements**

| Component         | Version   | Status           |
|-------------------|-----------|------------------|
| **Minecraft**     | 1.21.10+  |  Required        |
| **Fabric Loader** | 0.17.3+   |  Required        |
| **Fabric API**    | Latest    |  Required        |
| **Fabric Carpet** | 1.4.186+  |  Auto-downloaded |
| **LuckPerms**     | Any       |  Optional        |

### 🎮 Platform Support
- ✅ **Dedicated Servers** (Fully supported)
- ✅ **Single-Player** (Works perfectly)
- ✅ **LAN Worlds** (Host has OP privileges)
- ✅ **Multiplayer** (Client + Server installation)

### 🔗 Mod Integrations
- **Fabric Carpet**: Core dependency, auto-downloaded via JitPack
- **LuckPerms**: Optional advanced permission management
- **Other Mods**: Generally compatible, no known conflicts

## 📝 **Version History**

### 🚀 v2.0.0-1.21.10 (Current)
- ✅ **Full MC 1.21.10 compatibility** with latest Fabric
- ✅ **Auto-dependency resolution** via JitPack (no manual Carpet installation!)
- ✅ **Operator privilege system** - OPs can bypass bot limits
- ✅ **Fixed bot limit enforcement** - now works correctly for all players
- ✅ **Enhanced user commands** - `/cbr spawn` and `/cbr despawn` with smart naming
- ✅ **Improved permission system** - works without plugins, optional LuckPerms support
- ✅ **Better error handling** - clear messages when limits are reached
- ✅ **Cross-session persistence** - bot ownership survives server restarts

### 📜 Previous Versions
- **v1.2.0-1.21**: Fixed bot naming, simplified commands, added ALL despawn option
- **v1.1.0**: User-friendly commands and tab completion
- **v1.0.0**: Initial release with basic bot restrictions

### 🔄 Migration Notes
- **From v1.x**: Config settings are preserved, new permissions system is backward compatible
- **New installations**: No migration needed, works out of the box!

## 🐛 **Troubleshooting**

### 🚫 Common Issues & Solutions

**❌ "You cannot have more than X bots"**
- **Cause**: You've reached your bot limit
- **Solution**: Use `/cbr despawn` to remove existing bots, or ask an admin to increase your limit
- **Note**: Operators can bypass this limit

**❌ Bots not spawning**
- **Check**: Are you using the correct command? Try `/cbr spawn`
- **Check**: Do you have permission? (Should work by default)
- **Check**: Is the server running Fabric Carpet mod?

**❌ "Failed to spawn bot" errors**
- **Cause**: Usually a temporary server issue
- **Solution**: Wait a moment and try again
- **Note**: Check server console for detailed error messages

**❌ Tab completion not showing bots**
- **Cause**: Bots might not be online or you don't own them
- **Solution**: Only your own online bots appear in tab completion

**❌ Permission denied errors**
- **Default Setup**: Should work without any permission plugins
- **LuckPerms**: Check that permissions aren't explicitly denied
- **OP Issues**: Make sure you have the correct OP level (2 for config, 4 for unlimited bots)

### 📞 Getting Help

1. **Check Server Console**: Look for `carpetbotrestriction` log messages
2. **Verify Installation**: Ensure all required mods are present
3. **Test with OP**: Try the commands as an operator to isolate permission issues
4. **Check Config**: Look at `config/carpetbotrestriction/config.toml` for current settings

### 🔍 Debug Information

When reporting issues, please include:
- Minecraft version
- Fabric Loader version
- List of installed mods
- Full error message from logs
- Steps to reproduce the problem

## 🤝 **Contributing**

We welcome contributions to make this mod even better! Here's how you can help:

### 🐛 Bug Reports
- **Include**: Minecraft version, mod version, steps to reproduce
- **Logs**: Please include relevant server/client logs

### 💡 Feature Requests
- **Suggestions**: Open an issue with the `enhancement` label
- **Use Cases**: Explain how the feature would be useful
- **Discussion**: We love to discuss ideas before implementation

### 🔧 Development
- **Fork** the repository
- **Create** a feature branch
- **Test** your changes thoroughly
- **Submit** a pull request with clear description

### 📖 Documentation
- **README improvements**: Help make documentation clearer
- **Wiki contributions**: Add usage guides or troubleshooting tips
- **Translations**: Help translate the mod (contact us first)

## 📄 **License**

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

### 🙏 **Credits**
- **Fabric Team**: For the excellent modding framework
- **Carpet Mod Team**: For the amazing bot system
- **Community**: For feedback, testing, and contributions

---

**Made with ❤️ for the Minecraft community**