class_name LanMsg
## One decoded LAN datagram. Which fields are meaningful depends on [member type]:
## STATE fills the pose fields, CHAT fills text/target, BYE only carries the id.
## Wire-compatible with the Java client's com.deskcat.net.LanMsg.

const STATE := "S"
const CHAT := "C"
const BYE := "B"
const ACTION := "A"

# pet animation states carried in STATE messages
const ANIM_IDLE := 0
const ANIM_WALK := 1
const ANIM_SLEEP := 2
const ANIM_DRAG := 3
## Knocked over by a shot.
const ANIM_KO := 4

var type: String = ""
var id: String = ""
var name: String = ""
var skin: String = ""
## Window position as a fraction of the sender's usable screen.
var x_frac: float = 0.0
var y_frac: float = 0.0
var facing_left: bool = false
var anim: int = 0
var text: String = ""
## Empty for broadcast chat, a peer id for a DM.
var target: String = ""
## Bubble style carried with CHAT.
var chat_scale: float = 1.0
var chat_effect: int = 0
var chat_color: String = ""
## ACTION verb, e.g. "shoot".
var action: String = ""
## Set by the receiver when the datagram came from this same machine
## (another instance on this PC) — such peers get no mirror window.
var same_host: bool = false
