/** 列定义：key 与表格列的 column-key 对应，fixed 列不可隐藏。 */
export interface ColumnDef {
  key: string
  label: string
  fixed?: boolean
}
