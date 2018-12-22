package Assembler

//internal enum class Type {
//    inst, pseudo, asminst
////    ADD, AND, NOT,
////    BR, RET, JMP, JSR, JSRR, RTI,
////    LD, LDR, LDI, LEA, ST, STR, STI,
////    TRAP,
////    _START, _END, _FILL, _BLKW, _STRINGZ,
////    GETC, OUT, PUTS, IN, PUTSP, HALT
//}

/**
 * 字段类型枚举
 */
internal enum class WordType {
    Instruction,
    BR,     //BR指令需要特殊对待
    PseudoInstruction,
    AsmInstruction,
    Reg,
    Number,
    Label,
    String,
}

class Instruction(private val asm: Array<String>) {

    var label: String? = null
    var addr = -1
    var length = -1
    lateinit var symbolTable: HashMap<String, Int>

    private lateinit var Inst: String

    private var is_imm = false
    private var SR1 = -1
    private var SR2 = -1
    private var SR = -1
    private var DR = -1
    private var BaseR = -1
    private var imm = -1
    private var trapvector = -1
    private var string: String? = null

    init {
        //分析各个字段的类型
        val wordType = Array<WordType>(asm.size) { i ->
            //指令、伪指令、汇编指令和寄存器不区分大小写
            when (asm[i].toUpperCase()) {
                in InstTable -> WordType.Instruction
                in BRTable -> WordType.BR               //BR需要特殊对待
                in pseudoTable -> WordType.PseudoInstruction
                in asmInstTable -> WordType.AsmInstruction
                in regTable -> WordType.Reg
                else -> {
                    when (asm[i].first()) {
                        '#' -> WordType.Number
                        'x', 'X' -> {
                            //todo 判定数字
                            WordType.Number
                            //todo
                        }
                        '\'', '"' -> WordType.String
                        else -> WordType.Label
                    }
                }
            }
        }
        //todo 指令解析
    }

    /**
     * 转化为机器码
     */
    fun toMachineCode(): ShortArray {
        //Inst已经转化成了大写
        when (Inst) {
            in InstTable -> {
                var code = (InstTable[Inst] as Int) shl 12
                when (Inst) {
                    "ADD", "AND" -> {
                        code += DR shl 9
                        code += SR1 shl 6
                        if (is_imm) {
                            code += 1 shl 5
                            if (imm in -16..15) {
                                code += imm and 0b11_111
                            } else {
                                //todo 超出立即数表达范围
                                throw Exception()
                            }
                        } else {
                            code += SR2
                        }
                    }
                    "NOT" -> {
                        code += DR shl 9
                        code += SR1 shl 6
                    }
                    "JMP", "RET", "JSRR" -> {
                        code += BaseR shl 6
                    }
                    "JSR" -> {
                        code += 1 shl 11
                        if (imm in -1024..1023) {
                            code += imm and 0b11_111_111_111
                        } else {
                            //todo 超出立即数表达范围
                            throw Exception()
                        }
                    }
                    "RTI" -> {
                        //do nothing
                    }
                    "LD", "LDI", "LEA" -> {
                        code += DR shl 9
                        if (imm in -256..255) {
                            code += imm and 0b111_111_111
                        } else {
                            //todo 超出立即数表达范围
                            throw Exception()
                        }
                    }
                    "LDR" -> {
                        code += DR shl 9
                        code += BaseR shl 6
                        if (imm in -32..31) {
                            code += imm and 0b111_111
                        } else {
                            //todo 超出立即数表达范围
                            throw Exception()
                        }
                    }
                    "ST", "STI" -> {
                        code += SR shl 9
                        if (imm in -256..255) {
                            code += imm and 0b111_111_111
                        } else {
                            //todo 超出立即数表达范围
                            throw Exception()
                        }
                    }
                    "STR" -> {
                        code += SR shl 9
                        code += BaseR shl 6
                        if (imm in -32..31) {
                            code += imm and 0b111_111
                        } else {
                            //todo 超出立即数表达范围
                            throw Exception()
                        }
                    }
                    "TRAP" -> {
                        if (trapvector in 0..255) {
                            code += trapvector and 0b11_111_111
                        } else {
                            //todo 超出立即数表达范围
                            throw Exception()
                        }
                    }
                }
                return shortArrayOf(code.toShort())
            }
            in BRTable -> {
                var code = (BRTable[Inst] as Int) shl 9
                if (imm in -256..255) {
                    code += imm and 0b111_111_111
                } else {
                    //todo 超出立即数表达范围
                    throw Exception()
                }
                return shortArrayOf(code.toShort())
            }
            in pseudoTable -> {
                var code = 0b1111 shl 12
                code += (pseudoTable[Inst] as Int) and 0b11_111_111
                return shortArrayOf(code.toShort())
            }
            in asmInstTable -> {
                when (Inst) {
                    ".START", ".END" -> {
                        return shortArrayOf()
                    }
                    ".FILL" -> {
                        if (imm in -32768..32767) {
                            return shortArrayOf(imm.toShort())
                        } else {
                            //todo 超出立即数表达范围
                            throw Exception()
                        }
                    }
                    ".BLKW" -> {
                        if (imm >= 0) {
                            return ShortArray(imm) { 0 }
                        } else {
                            throw Exception()
                        }
                    }
                    ".STRINGZ" -> {
                        return ShortArray(imm) { i ->
                            string!![i].toShort()
                        }
                    }
                }

            }

        }
        //todo 非法指令
        throw Exception()
    }

}

internal fun parseNumber(str: String): Int {

}

internal fun parseString(str: String): String {

}

//指令与操作码对照表
internal val InstTable = hashMapOf(
        Pair("ADD", 0b0001),
        Pair("AND", 0b0101),
        Pair("NOT", 0b1001),
//        Pair("BR", 0b0000), //BR特殊对待
        Pair("RET", 0b1100),
        Pair("JMP", 0b1100),
        Pair("JSR", 0b0100),
        Pair("JSRR", 0b0100),
        Pair("RTI", 0b1000),
        Pair("LD", 0b0010),
        Pair("LDR", 0b0110),
        Pair("LDI", 0b1010),
        Pair("LEA", 0b1110),
        Pair("ST", 0b0011),
        Pair("STR", 0b0111),
        Pair("STI", 0b1011),
        Pair("TRAP", 0b1111)
)

//BR指令与nzp对照表
internal val BRTable = hashMapOf(
        Pair("BR", 0b000),
        Pair("BRN", 0b100),
        Pair("BRZ", 0b010),
        Pair("BRP", 0b001),
        Pair("BRNZ", 0b110),
        Pair("BRZN", 0b110),
        Pair("BRNP", 0b101),
        Pair("BRPN", 0b101),
        Pair("BRZP", 0b011),
        Pair("BRPZ", 0b011),
        Pair("BRNZP", 0b111),
        Pair("BRNPZ", 0b111),
        Pair("BRZNP", 0b111),
        Pair("BRZPN", 0b111),
        Pair("BRPNZ", 0b111),
        Pair("BRPZN", 0b111)
)

//伪指令与TRAP vector对照表
internal val pseudoTable = hashMapOf(
        Pair("GETC", 0x20),
        Pair("OUT", 0x21),
        Pair("PUTS", 0x22),
        Pair("IN", 0x23),
        Pair("PUTSP", 0x24),
        Pair("HALT", 0x25)
)

//寄存器对照表
internal val regTable = hashMapOf(
        Pair("R0", 0),
        Pair("R1", 1),
        Pair("R2", 2),
        Pair("R3", 3),
        Pair("R4", 4),
        Pair("R5", 5),
        Pair("R6", 6),
        Pair("R7", 7)
)

//汇编器指令枚举
internal enum class asmInstType {
    _START, _END, _FILL, _BLKW, _STRINGZ
}

//汇编器指令表
internal val asmInstTable = hashMapOf(
        Pair(".STRAT", asmInstType._START),
        Pair(".END", asmInstType._END),
        Pair(".FILL", asmInstType._FILL),
        Pair(".BLKW", asmInstType._BLKW),
        Pair(".STRINGZ", asmInstType._STRINGZ)
)
