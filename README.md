<img src="logo.png" alt="CC: Restitched" width="100%"/>

[![Current build status](https://github.com/cc-tweaked/cc-restitched/workflows/Build/badge.svg)](https://github.com/cc-tweaked/cc-restitched/actions "Current build status")
[![Download CC: Restitched on CurseForge](https://camo.githubusercontent.com/07622e6662ef5ead080c4840ef6514a34e079d63015f7e51c977a55b1881dfb9/687474703a2f2f63662e776179326d7563686e6f6973652e65752f7469746c652f63632d726573746974636865642e737667)](https://www.curseforge.com/minecraft/mc-mods/cc-restitched "Download CC:  Restitched on CurseForge")
[![Download CC: Restitched via Modrinth](https://img.shields.io/badge/dynamic/json?color=5da545&label=modrinth&prefix=status%20&query=status&url=https://api.modrinth.com/api/v1/mod/cc-restitched)](https://modrinth.com/mod/cc-restitched "Download CC:  Restitched on CurseForge")
[![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-908a85?logo=gitpod)](https://gitpod.io/#https://github.com/cc-tweaked/cc-restitched)

# What is CC: Restitched?
This is a port of [SquidDev-CC/CC-Tweaked](https://github.com/SquidDev-CC/CC-Tweaked) to the Fabric modloader as well as being a continuation of [JemmaZZ/cc-tweaked-fabric](https://github.com/JemmaZZ/cc-tweaked-fabric).

## CC: Restitched vs. CC: Tweaked
CC: R tries to maintain parity with CC: T, but may be behind or divergent in some areas. If you notice a disparity please open an issue. CC: R major and minor version numbers indicate parity with the major features of that version of CC: T. Patch version numbers will not align.

## Resource Packs
This mod includes textures by [Jummit](https://github.com/Jummit) that are more in line with the style of Mojang's new texture-artist, Jappa. If you prefer the original textures you should  enable the "Classic" resource pack.
<img src="https://raw.githubusercontent.com/cc-orgs/cc-overhaul/main/pack.png" alt="CC: Restitched" width="32"  height="32"/> We also have a third resourcepack made by [3prm3](https://github.com/3prm3), it features a complete overhaul of CC's textures (in the style of an industrial mod) and can be used by enabling the `overhaul` resource pack. You can also check out his resource pack over [here](https://github.com/cc-orgs/cc-overhaul/tree/main) as  well!

## Bleeding Edge Ver.
Bleeding edge builds can be found [here](https://github.com/cc-tweaked/cc-restitched/actions) at Github Actions.
In the .zip file there should be a `-dev` jar, a `-javadoc` jar, a `-sources-dev` jar, a `-sources` jar, and a "plain" jar (jar without an affixed tag) jar.
Put the "plain" jar in the mods folder.

## Contributions
Any contribution is welcome, be it using the mod, reporting bugs or contributing code. In order to start helping develop CC: R there are a few rules;
1) Follow the [Fabric](https://fabricmc.net/) programming guidelines as close as possible. This means you have to use [`loom`](https://fabricmc.net/wiki/tutorial:mappings) mappings, if you use anything else, your code will be rejected.
2) Make sure your code follows the checkstyle rules. You can test this by running `./gradle build` or `./gradle check`.
3) You cannot intentionally implement bugs and security vulnerabilities.
4) Unless the code is taken directly from CC: Tweaked, `lua` code is offlimits from alteration. If you wish to contribute your changes to the in game rom please contribute upstream at [CC-Tweaked](https://github.com/SquidDev-CC/CC-Tweaked).
5) Branches should be named in this format `mc-<major ver.>.x/<specific-mc-ver.>`

# Rendering Mod Compatability
* [ YES ] [Sodium](https://www.curseforge.com/minecraft/mc-mods/sodium)
* [ YES ] [OptiFabric](https://www.curseforge.com/minecraft/mc-mods/optifabric)
	* Works with VBO Rendering (automatically set)
	* No issues
* [ OK ] [Iris Shaders & Sodium](https://www.curseforge.com/minecraft/mc-mods/irisshaders)
	* "Works" with TBO Rendering (Default)
	* Works with VBO Rendering
* [ YES ] [Canvas](https://www.curseforge.com/minecraft/mc-mods/canvas-renderer)
	* Works with TBO Rendering (Default)
	* Scuffed with VBO Rendering
	* <details>
		<summary>VBO is broken</summary>

  		Monitors are just... scuffed beyond belief.
		- ![](https://i.imgur.com/JVNZ2Pn.png)
		- ![](https://i.imgur.com/SXXpr54.png)
			* The content to the left is supposed to be on the monitors to the right, also the bottom one is supposed to `black/white` not colored.
		* Turtle Texture for some reason?
			- ![](https://i.imgur.com/OEmZXsx.png)
		</details>

## Community
If you need help getting started with CC: Restitched, want to show off your latest project, or just want to chat about ComputerCraft, here is the [Computercraft Forum](https://forums.computercraft.cc/) and the Computer Mods [Discord!](https://discord.gg/H2UyJXe). There is also a wiki that can be found at [https://tweaked.cc/](https://tweaked.cc/)

## Perpheral Mods
Unfortunately, CC: Restitched does not have as many peripherals mods available as CC: Tweaked. If you're an interested mod developer, please check out our `api` package. If you've already made a mod with CC: R peripheral support OR if you're a player who found a mod with CC: R integration, please open an [issue here](https://github.com/cc-tweaked/cc-restitched/issues/new?assignees=&labels=peripheralShoutout&template=peripheral_shoutout.md) to let us know and we'll add it to the list!
