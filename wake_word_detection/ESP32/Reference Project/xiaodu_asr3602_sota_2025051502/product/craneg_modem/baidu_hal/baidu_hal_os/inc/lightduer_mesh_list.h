// Copyright (2019) Baidu Inc. All rights reserveed.
/**
 * File: lightduer_mesh_list.h
 * Auth: lingfeng meng (menglingfeng@baidu.com)
 * Desc: Duer mesh list functions.
 */
#ifndef BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_LIST_H
#define BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_LIST_H

typedef struct mesh_node_s {
    void *data;
    struct mesh_node_s *next;
} mesh_node_t;

typedef struct {
    void *(*malloc_node)(void *data);
    void (*free_node)(void *data);
} mesh_node_ops;

typedef struct mesh_list_s {
    int length;
    mesh_node_t *head;
    mesh_node_t *tail;
    void (*insert)(struct mesh_list_s *list, void *data);
    void *(*get_info)(struct mesh_list_s *list, int index);
    void (*remove_node)(struct mesh_list_s *list, int index);
    void (*destroy)(struct mesh_list_s *list);
    mesh_node_ops *ops;
} mesh_list_t;

void *init_list(mesh_node_ops *ops);
#endif /* BAIDU_DUER_LIGHTDUER_INCLUDE_LIGHTDUER_MESH_LIST_H */