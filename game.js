"use strict";

// ================================================================
//  CONFIG
// ================================================================
const API = "/api"; // same-origin — works on Render, Railway, local

const SQ = 75; // square size px

// Chess.com colours
const C = {
    light:    "#eeeed2",
    dark:     "#769656",
    lastL:    "#f6f669",
    lastD:    "#baca2b",
    selL:     "#f6f669",
    selD:     "#baca2b",
    dot:      "rgba(0,0,0,0.20)",
    ring:     "rgba(0,0,0,0.20)",
};

// Unicode pieces — index = piece type (1-6)
const WH = ["","♙","♘","♗","♖","♕","♔"];
const BL = ["","♟","♞","♝","♜","♛","♚"];

// ================================================================
//  GAME STATE
// ================================================================
let G = {
    board:       null,
    whiteTurn:   true,
    playerWhite: true,
    depth:       4,       // 3 = ~1000 ELO, 4 = ~1400 ELO
    selRow:      -1,
    selCol:      -1,
    legal:       [],
    lastMove:    null,
    history:     [],
    curMove:     -1,
    canUndo:     false,
    canRedo:     false,
    gameOver:    false,
    thinking:    false,
};

// ================================================================
//  CANVAS
// ================================================================
const canvas = document.getElementById("chess-board");
const ctx    = canvas.getContext("2d");
canvas.width = canvas.height = 600;

// ================================================================
//  DIALOG STATE — track selections before starting
// ================================================================
let dlgColor  = null;  // "white" or "black"
let dlgRating = null;  // 1000 or 1400

function selectColor(color) {
    dlgColor = color;
    document.querySelectorAll(".cbtn").forEach(b =>
        b.classList.toggle("selected", b.dataset.color === color));
    checkStartReady();
}

function selectRating(rating) {
    dlgRating = rating;
    document.querySelectorAll(".rbtn").forEach(b =>
        b.classList.toggle("selected", parseInt(b.dataset.rating) === rating));
    checkStartReady();
}

function checkStartReady() {
    document.getElementById("btn-start").disabled = !(dlgColor && dlgRating);
}

// ================================================================
//  START GAME
// ================================================================
async function startGame() {
    if (!dlgColor || !dlgRating) return;

    G.playerWhite = dlgColor === "white";
    G.depth       = dlgRating === 1000 ? 3 : 4;

    document.getElementById("dlg-color").style.display = "none";
    document.getElementById("game").classList.add("on");

    // Show engine info in right panel
    document.getElementById("ei-elo").textContent  = "~" + dlgRating + " ELO";
    document.getElementById("ei-desc").textContent =
        dlgRating === 1000 ? "Beginner Engine" : "Intermediate Engine";

    buildLabels();
    const res = await call("/new-game", {});
    if (!res.ok) {
        setStatus("Failed to start game", "");
        return;
    }
    applyState(res);

    // Engine moves first if player chose black
    if (!G.playerWhite) await doEngineMove();
}

// ================================================================
//  BUILD RANK/FILE LABELS
// ================================================================
function buildLabels() {
    const rankDiv = document.getElementById("ranks");
    const fileDiv = document.getElementById("files");
    rankDiv.innerHTML = fileDiv.innerHTML = "";

    for (let i = 0; i < 8; i++) {
        // Ranks: white sees 8 at top, black sees 1 at top
        const rank = G.playerWhite ? (8 - i) : (i + 1);
        const rl   = document.createElement("div");
        rl.className   = "rlbl";
        rl.textContent = rank;
        rankDiv.appendChild(rl);

        // Files: white sees a at left, black sees h at left
        const file = G.playerWhite
            ? String.fromCharCode(97 + i)
            : String.fromCharCode(104 - i);
        const fl   = document.createElement("div");
        fl.className   = "flbl";
        fl.textContent = file;
        fileDiv.appendChild(fl);
    }
}

// ================================================================
//  DRAW BOARD
//
//  The canvas always shows 8x8 cells.
//  canvasRow/canvasCol = what we draw at pixel position
//  boardRow/boardCol   = actual board array index
//
//  White: boardRow = canvasRow,     boardCol = canvasCol
//  Black: boardRow = 7 - canvasRow, boardCol = 7 - canvasCol
// ================================================================
function draw() {
    if (!G.board) return;
    ctx.clearRect(0, 0, 600, 600);

    for (let cr = 0; cr < 8; cr++) {
        for (let cc = 0; cc < 8; cc++) {
            const br = G.playerWhite ? cr : (7 - cr);
            const bc = G.playerWhite ? cc : (7 - cc);
            const x  = cc * SQ;
            const y  = cr * SQ;
            const light = (cr + cc) % 2 === 0;

            // 1. base square
            ctx.fillStyle = light ? C.light : C.dark;
            ctx.fillRect(x, y, SQ, SQ);

            // 2. last move highlight
            if (G.lastMove) {
                const lm = G.lastMove;
                if ((br===lm.fromRow&&bc===lm.fromCol)||(br===lm.toRow&&bc===lm.toCol)) {
                    ctx.fillStyle = light ? C.lastL : C.lastD;
                    ctx.fillRect(x, y, SQ, SQ);
                }
            }

            // 3. selected piece highlight
            if (br===G.selRow && bc===G.selCol) {
                ctx.fillStyle = light ? C.selL : C.selD;
                ctx.fillRect(x, y, SQ, SQ);
            }

            // 4. legal move indicators
            const isLegal = G.legal.some(m => m.row===br && m.col===bc);
            if (isLegal) {
                if (G.board[br][bc] === 0) {
                    // dot on empty square
                    ctx.fillStyle = C.dot;
                    ctx.beginPath();
                    ctx.arc(x+SQ/2, y+SQ/2, SQ*0.15, 0, Math.PI*2);
                    ctx.fill();
                } else {
                    // ring on capture square
                    ctx.strokeStyle = C.ring;
                    ctx.lineWidth   = 6;
                    ctx.beginPath();
                    ctx.arc(x+SQ/2, y+SQ/2, SQ/2-3, 0, Math.PI*2);
                    ctx.stroke();
                }
            }

            // 5. draw piece
            const piece = G.board[br][bc];
            if (piece !== 0) drawPiece(piece, x, y);
        }
    }
}

function drawPiece(piece, x, y) {
    const white  = piece > 0;
    const type   = Math.abs(piece);
    const symbol = white ? WH[type] : BL[type];

    ctx.save();
    ctx.font         = "52px 'Segoe UI Symbol','Apple Color Emoji',serif";
    ctx.textAlign    = "center";
    ctx.textBaseline = "middle";

    const cx = x + SQ/2;
    const cy = y + SQ/2 + 2;

    // outline for contrast
    ctx.fillStyle = white ? "rgba(0,0,0,0.4)" : "rgba(255,255,255,0.15)";
    ctx.fillText(symbol, cx+1, cy+1);

    ctx.fillStyle = white ? "#ffffff" : "#1a1a1a";
    ctx.fillText(symbol, cx, cy);
    ctx.restore();
}

// ================================================================
//  CLICK HANDLER
// ================================================================
canvas.addEventListener("click", async e => {
    if (G.gameOver || G.thinking) return;
    if (G.whiteTurn !== G.playerWhite) return; // not player's turn

    const rect  = canvas.getBoundingClientRect();
    const scaleX = canvas.width  / rect.width;
    const scaleY = canvas.height / rect.height;

    // canvas cell clicked
    let cc = Math.floor((e.clientX - rect.left) * scaleX / SQ);
    let cr = Math.floor((e.clientY - rect.top)  * scaleY / SQ);
    cc = Math.max(0, Math.min(7, cc));
    cr = Math.max(0, Math.min(7, cr));

    // convert to board coordinates
    const bc = G.playerWhite ? cc : (7 - cc);
    const br = G.playerWhite ? cr : (7 - cr);

    if (G.selRow === -1) {
        // first click: select own piece
        const piece = G.board[br][bc];
        if (piece !== 0 && (piece > 0) === G.playerWhite) {
            G.selRow = br; G.selCol = bc;
            await fetchLegal(br, bc);
        }
    } else {
        // second click: move or reselect
        const dest = G.legal.find(m => m.row===br && m.col===bc);
        if (dest) {
            await doPlayerMove(G.selRow, G.selCol, br, bc);
            G.selRow=-1; G.selCol=-1; G.legal=[];
        } else {
            const piece = G.board[br][bc];
            if (piece !== 0 && (piece > 0) === G.playerWhite) {
                G.selRow = br; G.selCol = bc;
                await fetchLegal(br, bc);
            } else {
                G.selRow=-1; G.selCol=-1; G.legal=[];
                draw();
            }
        }
    }
});

// ================================================================
//  API CALLS
// ================================================================
async function call(endpoint, data) {
    try {
        const r = await fetch(API + endpoint, {
            method:  "POST",
            headers: {"Content-Type":"application/json"},
            body:    JSON.stringify(data),
        });
        return await r.json();
    } catch(err) {
        console.error("API error:", endpoint, err);
        setStatus("Connection error", "");
        return {ok: false, error: String(err)};
    }
}

async function fetchLegal(row, col) {
    const res = await call("/legal-moves", {row, col});
    G.legal   = res.moves || [];
    draw();
}

async function doPlayerMove(fr, fc, tr, tc) {
    setStatus("...", "");
    const res = await call("/player-move", {fromRow:fr,fromCol:fc,toRow:tr,toCol:tc});
    if (!res.ok) {
        setStatus("Illegal move!", "");
        G.selRow=-1; G.selCol=-1; G.legal=[];
        draw();
        return;
    }
    applyState(res);
    if (G.gameOver) return;
    await doEngineMove();
}

async function doEngineMove() {
    G.thinking = true;
    setStatus("Engine thinking…", "thinking");
    draw();
    const res  = await call("/engine-move", {depth: G.depth});
    G.thinking = false;
    if (!res.ok) { setStatus("Engine error",""); return; }
    applyState(res);
}

// ================================================================
//  APPLY STATE
// ================================================================
function applyState(res) {
    const b     = res.board;
    G.board     = b.squares;
    G.whiteTurn = b.whiteTurn;
    G.lastMove  = res.lastMove || null;
    G.history   = res.history  || [];
    G.canUndo   = res.canUndo;
    G.canRedo   = res.canRedo;
    G.curMove   = -1;

    updateButtons();
    renderNotation();
    handleStatus(res.status);
    draw();
}

// ================================================================
//  STATUS
// ================================================================
function handleStatus(status) {
    switch(status) {
        case "checkmate": {
            // side with no moves is mated = current whiteTurn side
            const playerLost = G.whiteTurn === G.playerWhite;
            G.gameOver = true;
            showResult("♟", playerLost ? "You Lose!" : "You Win!", "Checkmate");
            break;
        }
        case "stalemate":
            G.gameOver = true;
            showResult("🤝", "Draw!", "Stalemate");
            break;
        case "check": {
            const playerInCheck = G.whiteTurn === G.playerWhite;
            setStatus(playerInCheck ? "⚠ You are in check!" : "Check!", "check");
            break;
        }
        default:
            setStatus(G.whiteTurn === G.playerWhite ? "Your turn" : "Engine's turn", "");
    }
}

function setStatus(txt, cls) {
    const el = document.getElementById("status");
    el.textContent = txt;
    el.className   = cls;
}

// ================================================================
//  NOTATION PANEL
// ================================================================
function renderNotation() {
    const list = document.getElementById("moves-list");
    list.innerHTML = "";

    if (G.history.length === 0) {
        list.innerHTML = "<div style='color:#555;font-size:12px;padding:4px'>No moves yet</div>";
        return;
    }

    for (let i = 0; i < G.history.length; i += 2) {
        const num   = Math.floor(i/2) + 1;
        const white = G.history[i]     || "";
        const black = G.history[i+1]   || "";

        const row  = document.createElement("div");
        row.className = "mrow";

        const nEl = document.createElement("div");
        nEl.className = "mnum";
        nEl.textContent = num + ".";

        const wEl = document.createElement("div");
        wEl.className   = "mcell" + (isCur(i) ? " cur" : "");
        wEl.textContent = white;
        wEl.onclick     = () => { G.curMove=i; renderNotation(); };

        const bEl = document.createElement("div");
        bEl.className   = "mcell" + (isCur(i+1) ? " cur" : "");
        bEl.textContent = black;
        if (black) bEl.onclick = () => { G.curMove=i+1; renderNotation(); };

        row.append(nEl, wEl, bEl);
        list.appendChild(row);
    }
    list.scrollTop = list.scrollHeight;
}

function isCur(idx) {
    if (G.curMove !== -1) return G.curMove === idx;
    return idx === G.history.length - 1;
}

// ================================================================
//  CONTROLS
// ================================================================
async function handleUndo() {
    if (!G.canUndo || G.thinking) return;
    G.gameOver = false;
    const res  = await call("/undo", {});
    if (res.ok) applyState(res);
}

async function handleRedo() {
    if (!G.canRedo || G.thinking) return;
    const res = await call("/redo", {});
    if (res.ok) applyState(res);
}

function handleResign() {
    if (G.gameOver || G.thinking) return;
    G.gameOver = true;
    showResult("⚑", "You Resigned", "Engine wins");
}

function updateButtons() {
    document.getElementById("btn-undo").disabled   = !G.canUndo;
    document.getElementById("btn-redo").disabled   = !G.canRedo;
    document.getElementById("btn-resign").disabled = G.gameOver;
}

// ================================================================
//  RESULT DIALOG
// ================================================================
function showResult(icon, title, sub) {
    document.getElementById("r-icon").textContent  = icon;
    document.getElementById("r-title").textContent = title;
    document.getElementById("r-sub").textContent   = sub;
    document.getElementById("dlg-result").classList.add("on");
}

function newGame() {
    document.getElementById("dlg-result").classList.remove("on");
    document.getElementById("game").classList.remove("on");
    document.getElementById("dlg-color").style.display = "flex";

    // reset
    dlgColor = null; dlgRating = null;
    document.querySelectorAll(".cbtn,.rbtn").forEach(b => b.classList.remove("selected"));
    document.getElementById("btn-start").disabled = true;

    Object.assign(G, {
        board:null, whiteTurn:true, playerWhite:true, depth:4,
        selRow:-1, selCol:-1, legal:[],
        lastMove:null, history:[], curMove:-1,
        canUndo:false, canRedo:false, gameOver:false, thinking:false,
    });

    ctx.clearRect(0, 0, 600, 600);
    document.getElementById("moves-list").innerHTML = "";
    setStatus("Your turn", "");
}
